package ai.typesafe.voicebrowser.controller;

import ai.typesafe.voicebrowser.model.Constants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class StateApiController {

    private final Controller controller;

    public StateApiController(Controller controller) {
        this.controller = controller;
    }

    @GetMapping("/state")
    public Map<String, Object> getState() {
        return controller.uiState();
    }

    @GetMapping("/questions")
    public Map<String, Object> getQuestions() {
        return Map.of(
                "model", Constants.MODEL,
                "thresholds", Constants.T
        );
    }
}
