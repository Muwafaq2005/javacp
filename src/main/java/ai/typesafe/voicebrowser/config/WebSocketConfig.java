package ai.typesafe.voicebrowser.config;

import ai.typesafe.voicebrowser.controller.Controller;
import ai.typesafe.voicebrowser.controller.WebSocketsHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final Controller controller;

    public WebSocketConfig(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketsHandler(controller), "/ws", "/ws/")
                .setAllowedOrigins("*");
    }
}
