package ai.typesafe.voicebrowser.config;

import ai.typesafe.voicebrowser.controller.Controller;
import ai.typesafe.voicebrowser.controller.WebSocketsHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final Controller controller;
    private final String[] allowedOrigins;

    public WebSocketConfig(Controller controller,
                           @Value("${voicebrowser.allowed-origins:http://localhost:[*],http://127.0.0.1:[*]}") String[] allowedOrigins) {
        this.controller = controller;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Browsers don't apply CORS to WebSockets, so without an origin check any website could drive the browser
        registry.addHandler(new WebSocketsHandler(controller), "/ws", "/ws/")
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
