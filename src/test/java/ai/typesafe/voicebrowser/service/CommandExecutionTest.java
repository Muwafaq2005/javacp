package ai.typesafe.voicebrowser.service;

import ai.typesafe.voicebrowser.model.PolicyResult;
import ai.typesafe.voicebrowser.service.JevClient.JevDecisionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CommandExecutionTest {

    @Test
    @DisplayName("Should evaluate media control voice commands")
    void testMediaControlHeuristic() {
        JevDecisionResponse resp = JevClient.decide("pause", Collections.emptyList(), "https://youtube.com", "YouTube", "youtube", Collections.emptyMap());
        assertNotNull(resp);
        assertEquals("media_control", resp.answers().get("intent").get("choice"));

        PolicyResult pol = PolicyEngine.evaluatePolicy(
                resp.answers(),
                resp.candidates(),
                Collections.emptyList(),
                "https://youtube.com",
                "YouTube",
                "youtube",
                null,
                100,
                true,
                null,
                Collections.emptyList()
        );

        assertEquals("act", pol.getDecision());
        assertEquals("media_control", pol.getAction().getType());
    }

    @Test
    @DisplayName("Should evaluate scroll top and scroll bottom voice commands")
    void testScrollTopBottomHeuristics() {
        JevDecisionResponse topResp = JevClient.decide("scroll to top", Collections.emptyList(), "https://example.com", "Example", "example_com", Collections.emptyMap());
        assertEquals("scroll_top", topResp.answers().get("intent").get("choice"));

        JevDecisionResponse botResp = JevClient.decide("scroll to bottom", Collections.emptyList(), "https://example.com", "Example", "example_com", Collections.emptyMap());
        assertEquals("scroll_bottom", botResp.answers().get("intent").get("choice"));
    }

    @Test
    @DisplayName("Should evaluate press key voice commands")
    void testPressKeyHeuristics() {
        JevDecisionResponse resp = JevClient.decide("press enter", Collections.emptyList(), "https://example.com", "Example", "example_com", Collections.emptyMap());
        assertEquals("press_key", resp.answers().get("intent").get("choice"));
    }

    @Test
    @DisplayName("Should evaluate tab management voice commands")
    void testTabCommandsHeuristics() {
        JevDecisionResponse newTabResp = JevClient.decide("open new tab", Collections.emptyList(), "https://example.com", "Example", "example_com", Collections.emptyMap());
        assertEquals("open_new_tab", newTabResp.answers().get("intent").get("choice"));

        JevDecisionResponse closeTabResp = JevClient.decide("close tab", Collections.emptyList(), "https://example.com", "Example", "example_com", Collections.emptyMap());
        assertEquals("close_tab", closeTabResp.answers().get("intent").get("choice"));
    }
}
