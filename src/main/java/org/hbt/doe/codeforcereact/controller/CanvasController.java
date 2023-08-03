package org.hbt.doe.codeforcereact.controller;

import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import java.net.URI;

public class CanvasController {

    private void buildWebsocketSession() {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        client.execute(URI.create("ws://URL"), session -> {
                    return session
                            // Send our data
                            .send(/* */)
                            // Listen for data and log it
                            .thenMany(/* */)
                            // Tell the client we are done setting things up
                            .then();
                })
                // Keep things running forever
                .subscribe();
    }
}
