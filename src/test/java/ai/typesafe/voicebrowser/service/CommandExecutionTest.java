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

    @Test
    @DisplayName("Should evaluate ordinal selection voice commands")
    void testSelectOrdinalHeuristics() {
        JevDecisionResponse resp = JevClient.decide("click the 3rd video", Collections.emptyList(), "https://youtube.com", "YouTube", "youtube", Map.of("text", List.of("click the 3rd video")));
        assertEquals("select_ordinal", resp.answers().get("intent").get("choice"));

        PolicyResult pol = PolicyEngine.evaluatePolicy(
                resp.answers(),
                Map.of("text", List.of("click the 3rd video")),
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
        assertEquals("select_ordinal", pol.getAction().getType());
        assertEquals(3, pol.getAction().getOrdinalIndex());
    }

    @Test
    @DisplayName("Should evaluate volume and seek media control voice commands")
    void testVolumeAndSeekHeuristics() {
        JevDecisionResponse volResp = JevClient.decide("volume up", Collections.emptyList(), "https://youtube.com", "YouTube", "youtube", Map.of("text", List.of("volume up")));
        assertEquals("volume_control", volResp.answers().get("intent").get("choice"));

        PolicyResult volPol = PolicyEngine.evaluatePolicy(
                volResp.answers(),
                Map.of("text", List.of("volume up")),
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
        assertEquals("act", volPol.getDecision());
        assertEquals("volume_up", volPol.getAction().getMediaCommand());

        JevDecisionResponse seekResp = JevClient.decide("skip 10 seconds", Collections.emptyList(), "https://youtube.com", "YouTube", "youtube", Map.of("text", List.of("skip 10 seconds")));
        assertEquals("seek_media", seekResp.answers().get("intent").get("choice"));
    }
}
