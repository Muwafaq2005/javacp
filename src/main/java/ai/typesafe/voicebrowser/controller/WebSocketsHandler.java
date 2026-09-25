package ai.typesafe.voicebrowser.controller;

import ai.typesafe.voicebrowser.browser.Controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebSocketsHandler extends TextWebSocketHandler {

    private final Controller controller;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    public WebSocketsHandler(Controller controller) {
        this.controller = controller;
        this.controller.onEvent(msg -> broadcast(msg));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        send(session, Map.of("type", "hello", "payload", controller.uiState()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = mapper.readValue(message.getPayload(), Map.class);
            String type = (String) msg.get("type");
            if (type == null) return;

            switch (type) {
                case "transcript":
                    String text = (String) msg.get("text");
                    Boolean isFinal = (Boolean) msg.getOrDefault("final", false);
                    String uttId = String.valueOf(msg.getOrDefault("utteranceId", System.currentTimeMillis()));
                    controller.handleTranscript(text, isFinal, uttId);
                    break;
                case "command":
                    String cmdText = (String) msg.get("text");
                    controller.handleCommand(cmdText);
                    break;
                case "undo":
                    controller.undo();
                    break;
                case "snapshot":
                    controller.refreshSnapshot();
                    break;
                case "state":
                    send(session, Map.of("type", "hello", "payload", controller.uiState()));
                    break;
            }
        } catch (Exception ignored) {}
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        if (!session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
        } catch (IOException ignored) {}
    }

    private void broadcast(Map<String, Object> payload) {
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(msg);
                } catch (IOException ignored) {}
            }
        }
    }
}
