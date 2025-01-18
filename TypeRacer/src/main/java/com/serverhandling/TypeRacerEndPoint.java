package com.serverhandling;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/typeracer/{roomId}")
public class TypeRacerEndPoint {

    private static final Map<String, Room> rooms = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Queue<Session>> waitingList = new ConcurrentHashMap<>();
    public static final Map<String, String> textForRoom = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") String roomId) throws IOException {
        System.out.println("Connection opened. Room ID: " + roomId + ", Session ID: " + session.getId());
        
        // Retrieve or generate text for the room
        String text = textForRoom.computeIfAbsent(roomId, id -> GenerateText.get());

        // Retrieve or create the room
        Room room = rooms.computeIfAbsent(roomId, id -> new Room(id, text));

        // Handle client connection
        if (room.isGameStarted()) {
            // Add client to the waiting list if the game has already started
            waitingList.computeIfAbsent(roomId, k -> new LinkedList<>()).add(session);
            session.getBasicRemote().sendText("WAIT");
        } else {
            // Add client to the room
            room.addClient(session);
            session.getBasicRemote().sendText("TEXT:" + text);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.removeClient(session);
            System.out.println("Connection closed. Room ID: " + roomId + ", Session ID: " + session.getId());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("roomId") String roomId) throws IOException {
        Room room = rooms.get(roomId);

        if (room != null) {
            if (message.startsWith("READY")) {
                // Mark the client as ready
                room.setClientReady(session);

                // Check if all clients are ready and start the game
                if (room.checkAllClientsReady()) {
                    room.startGame();
                    room.broadcastMessage("START");
                }
            } else if (message.startsWith("RESULT")) {
                // Process result and update the text for the room
                textForRoom.put(roomId, GenerateText.get());
                room.setClientFinished(session);
                room.broadcastMessage(message);

                // Check if all clients are finished or if the game time has ended
                if (room.areAllClientsFinished() || message.endsWith("time")) {
                    room.resetGame();
                    room.broadcastMessage("REMATCH");
                    processWaitingList(roomId);
                }
            } else if (message.startsWith("PROGRESS:")) {
                // Broadcast progress for a specific client
                room.broadcastProgress(session, message.substring(9));
            } else {
                // General message handling
                room.broadcastMessage(session.getId() + ": " + message);
                System.out.println("Message in Room ID: " + roomId + ", Session ID: " + session.getId() + ", Message: " + message);
            }
        }
    }

    private void processWaitingList(String roomId) {
        Queue<Session> queue = waitingList.get(roomId);
        Room room = rooms.get(roomId);

        if (queue != null && room != null) {
            while (!queue.isEmpty()) {
                Session session = queue.poll();
                try {
                    if (session.isOpen()) {
                        room.addClient(session);
                        session.getBasicRemote().sendText("ENTER");
                    }
                } catch (IOException e) {
                    System.err.println("Error adding client from waiting list to room. Session ID: " + session.getId());
                    e.printStackTrace();
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error occurred for Session ID: " + session.getId());
        throwable.printStackTrace();
    }
}
