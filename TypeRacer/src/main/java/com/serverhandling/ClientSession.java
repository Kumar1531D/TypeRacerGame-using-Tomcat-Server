package com.serverhandling;

import java.io.OutputStream;

public class ClientSession {
	private final String sessionId;
    private final OutputStream output;

    public ClientSession(String sessionId, OutputStream output) {
        this.sessionId = sessionId;
        this.output = output;
    }

    public String getSessionId() {
        return sessionId;
    }

    public OutputStream getOutput() {
        return output;
    }
}
