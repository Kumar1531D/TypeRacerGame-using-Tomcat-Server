package com.serverhandling;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.websocket.Session;

public class Room {

    private final String roomId;
    private final Map<Session, Boolean> clients = Collections.synchronizedMap(new HashMap<>());
    private final Map<Session, Boolean> finishedClients = Collections.synchronizedMap(new HashMap<>());
    private final String textContent;
    private boolean gameStarted = false;

    // Constructor
    public Room(String roomId, String textContent) {
        this.roomId = roomId;
        this.textContent = textContent;
    }

    // Getters
    public String getRoomId() {
        return roomId;
    }

    public String getTextContent() {
        return textContent;
    }

    // Add a client to the room
    public void addClient(Session session) {
        if (gameStarted) {
            sendMessageToClient(session, "ROOM_IN_GAME");
            return;
        }
        clients.put(session, false); // Not ready by default
        finishedClients.put(session, false); // Not finished by default
        sendMessageToClient(session, "TEXT:" + textContent);
    }

    // Remove a client from the room
    public void removeClient(Session session) {
        clients.remove(session);
        finishedClients.remove(session);
    }

    // Set a client as ready
    public void setClientReady(Session session) {
        clients.put(session, true);
    }

    // Check if all clients are ready
    public boolean checkAllClientsReady() {
        for (boolean isReady : clients.values()) {
            if (!isReady) {
                return false;
            }
        }
        return true;
    }

    // Start the game if all clients are ready
    public void startGameIfReady() {
        if (checkAllClientsReady()) {
            startGame();
        }
    }

    // Mark a client as finished
    public void setClientFinished(Session session) {
        finishedClients.put(session, true);
    }

    // Check if all clients are finished
    public boolean areAllClientsFinished() {
        for (Boolean isFinished : finishedClients.values()) {
            if (!isFinished) {
                return false;
            }
        }
        return true;
    }

    // Start the game
    public void startGame() {
        gameStarted = true;
        broadcastMessage("GAME_STARTED");
    }

    // Reset the game
    public void resetGame() {
        gameStarted = false;
        synchronized (clients) {
            for (Session session : clients.keySet()) {
                clients.put(session, false); // Reset readiness
                finishedClients.put(session, false); // Reset finished status
            }
        }
    }

    // Check if the game has started
    public boolean isGameStarted() {
        return gameStarted;
    }

    // Broadcast progress of a specific client
    public void broadcastProgress(Session session, String progress) {
    	String message = "PROGRESS:" + session.getId() + ":" + progress;
        synchronized (clients) {
            for (Session client : clients.keySet()) {
                if (client.isOpen()) {
                    try {
                        client.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Broadcast a message to all clients
    public void broadcastMessage(String message) {
        synchronized (clients) {
            for (Session client : clients.keySet()) {
                sendMessageToClient(client, message);
            }
        }
    }

    // Send a message to a specific client
    private void sendMessageToClient(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
        }
    }
}
