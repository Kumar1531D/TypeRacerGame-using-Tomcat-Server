package com.serverhandling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WebSocketConnectionHandler implements Runnable {
	private static final String MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Map<String, Room> rooms = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, String> textForRoom = new HashMap<>();
    private final Socket socket;
    private final ClientSession session;
    private final String roomId;

    public WebSocketConnectionHandler(Socket socket, ClientSession session, String roomId) {
        this.socket = socket;
        this.session = session;
        this.roomId = roomId;
    }

    @Override
    public void run() {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

            // Handle WebSocket handshake
            String data;
            StringBuilder request = new StringBuilder();
            while (!(data = reader.readLine()).isEmpty()) {
                request.append(data).append("\r\n");
            }

            String[] headers = request.toString().split("\r\n");
            String webSocketKey = null;

            for (String header : headers) {
                if (header.startsWith("Sec-WebSocket-Key:")) {
                    webSocketKey = header.split(":")[1].trim();
                }
            }

            if (webSocketKey == null) {
                return;
            }

            String acceptKey = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-1")
                            .digest((webSocketKey + MAGIC_STRING).getBytes(StandardCharsets.UTF_8)));

            writer.write("HTTP/1.1 101 Switching Protocols\r\n");
            writer.write("Upgrade: websocket\r\n");
            writer.write("Connection: Upgrade\r\n");
            writer.write("Sec-WebSocket-Accept: " + acceptKey + "\r\n");
            writer.write("\r\n");
            writer.flush();

            // Add the session to the room
            addClientToRoom(session, roomId);

            // Handle WebSocket frames
            handleWebSocketFrames(input, output);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleWebSocketFrames(InputStream input, OutputStream output) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(input);

        while (true) {
            byte firstByte = dataInputStream.readByte();
            boolean isFinalFrame = (firstByte & 0x80) != 0;
            int opcode = firstByte & 0x0F;

            byte secondByte = dataInputStream.readByte();
            boolean isMasked = (secondByte & 0x80) != 0;
            int payloadLength = secondByte & 0x7F;

            if (payloadLength == 126) {
                payloadLength = dataInputStream.readUnsignedShort();
            } else if (payloadLength == 127) {
                payloadLength = (int) dataInputStream.readLong();
            }

            byte[] maskingKey = new byte[4];
            if (isMasked) {
                dataInputStream.readFully(maskingKey);
            }

            byte[] payloadData = new byte[payloadLength];
            dataInputStream.readFully(payloadData);

            if (isMasked) {
                for (int i = 0; i < payloadLength; i++) {
                    payloadData[i] ^= maskingKey[i % 4];
                }
            }

            String message = new String(payloadData, StandardCharsets.UTF_8);
            System.out.println("Received message: " + message);

            // Process the message
            handleMessage(opcode, message);

            if (isFinalFrame) {
                break;
            }
        }
    }

    private void handleMessage(int opcode, String message) throws IOException {
        Room room = rooms.get(roomId);

        if (room != null) {
            if (message.startsWith("READY")) {
                room.setClientReady(session);
                if (room.checkAllClientsReady()) {
                    room.broadcastMessage("START");
                }
            } else if (message.startsWith("RESULT")) {
                textForRoom.put(roomId, GenerateText.get());
                room.setClientFinished(session);
                room.broadcastMessage(message);
//                InsertDAO in = new InsertDAO();
//                in.addHistory(GetName.get(message), in.getHistory(GetName.get(message)) + "\n" + message);

                if (room.areAllClientsFinished() || message.endsWith("time")) {
                    System.out.println("Inside button");
                    room.resetGame();
                    room.broadcastMessage("REMATCH");
                    processWaitingList(roomId);
                }
            } else if (message.startsWith("PROGRESS:")) {
                room.broadcastProgress(session.getSessionId(), message.substring(9));
            } else {
                room.broadcastMessage(session.getSessionId() + " : " + message);
                System.out.println("Message received in Room " + roomId + " : " + message + " from " + session.getSessionId());
            }
        }
    }

    private void processWaitingList(String roomId) {
        // Implement the logic to process the waiting list if needed
    }

    private void sendMessage(String message, OutputStream output) throws IOException {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        int messageLength = messageBytes.length;

        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(0x81); // Text frame with FIN bit set
        if (messageLength <= 125) {
            frame.write(messageLength);
        } else if (messageLength <= 65535) {
            frame.write(126);
            frame.write((messageLength >> 8) & 0xFF);
            frame.write(messageLength & 0xFF);
        } else {
            frame.write(127);
            frame.write((messageLength >> 56) & 0xFF);
            frame.write((messageLength >> 48) & 0xFF);
            frame.write((messageLength >> 40) & 0xFF);
            frame.write((messageLength >> 32) & 0xFF);
            frame.write((messageLength >> 24) & 0xFF);
            frame.write((messageLength >> 16) & 0xFF);
            frame.write((messageLength >> 8) & 0xFF);
            frame.write(messageLength & 0xFF);
        }
        frame.write(messageBytes);

        output.write(frame.toByteArray());
        output.flush();
    }

    private void addClientToRoom(ClientSession session, String roomId) {
        Room room = rooms.computeIfAbsent(roomId, id -> new Room(id, GenerateText.get()));
        room.addClient(session);
    }
}
