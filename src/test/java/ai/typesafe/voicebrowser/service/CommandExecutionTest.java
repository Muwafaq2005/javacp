package ai.typesafe.voicebrowser.service;

import ai.typesafe.voicebrowser.model.ElementSnapshot;
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

    @Test
    @DisplayName("Should evaluate playback speed and YouTube specific voice commands")
    void testPlaybackSpeedAndYouTubeActions() {
        JevDecisionResponse speedResp = JevClient.decide("speed 1.5x", Collections.emptyList(), "https://youtube.com", "YouTube", "youtube", Map.of("text", List.of("speed 1.5x")));
        assertEquals("playback_speed", speedResp.answers().get("intent").get("choice"));

        PolicyResult speedPol = PolicyEngine.evaluatePolicy(
                speedResp.answers(),
                Map.of("text", List.of("speed 1.5x")),
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
        assertEquals("act", speedPol.getDecision());
        assertEquals(1.5, speedPol.getAction().getPlaybackRate());

        JevDecisionResponse ccResp = JevClient.decide("turn on captions", Collections.emptyList(), "https://youtube.com", "YouTube", "youtube", Map.of("text", List.of("turn on captions")));
        assertEquals("youtube_action", ccResp.answers().get("intent").get("choice"));

        PolicyResult ccPol = PolicyEngine.evaluatePolicy(
                ccResp.answers(),
                Map.of("text", List.of("turn on captions")),
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
        assertEquals("act", ccPol.getDecision());
        assertEquals("cc", ccPol.getAction().getYtCommand());
    }

    private static PolicyResult policyFor(JevDecisionResponse resp, List<ElementSnapshot> elements, ai.typesafe.voicebrowser.model.Action pending) {
        return PolicyEngine.evaluatePolicy(resp.answers(), resp.candidates(), elements, "https://shop.example.com", "Shop", "generic",
                null, 100, true, pending, Collections.emptyList());
    }

    @Test
    @DisplayName("Heuristic click resolves to click_element on the element named in the transcript")
    void testHeuristicClickTargetsNamedElement() {
        List<ElementSnapshot> els = List.of(new ElementSnapshot("e01", "link", "Home"), new ElementSnapshot("e02", "link", "Documentation"));
        JevDecisionResponse resp = JevClient.decide("click documentation", els, "https://shop.example.com", "Shop", "generic", Collections.emptyMap());
        assertEquals("click_element", resp.answers().get("intent").get("choice"));

        PolicyResult pol = policyFor(resp, els, null);
        assertEquals("act", pol.getDecision());
        assertEquals("click_element", pol.getAction().getType());
        assertEquals("e02", pol.getAction().getTargetId());
    }

    @Test
    @DisplayName("Heuristic click with no matching element waits instead of clicking the first element")
    void testHeuristicClickWithoutMatchDoesNotGuess() {
        List<ElementSnapshot> els = List.of(new ElementSnapshot("e01", "link", "Home"));
        JevDecisionResponse resp = JevClient.decide("click pricing", els, "https://shop.example.com", "Shop", "generic", Collections.emptyMap());
        assertNotEquals("act", policyFor(resp, els, null).getDecision());
    }

    @Test
    @DisplayName("Destructive click asks for confirmation, and spoken confirm executes it")
    void testDestructiveClickNeedsConfirm() {
        List<ElementSnapshot> els = List.of(new ElementSnapshot("e01", "button", "Place order"));
        JevDecisionResponse resp = JevClient.decide("click place order", els, "https://shop.example.com", "Shop", "generic", Collections.emptyMap());
        PolicyResult pol = policyFor(resp, els, null);
        assertEquals("confirm", pol.getDecision());

        JevDecisionResponse yes = JevClient.decide("confirm", els, "https://shop.example.com", "Shop", "generic", Collections.emptyMap());
        PolicyResult confirmed = policyFor(yes, els, pol.getAction());
        assertEquals("act", confirmed.getDecision());
        assertEquals("e01", confirmed.getAction().getTargetId());

        assertEquals("ignore", policyFor(yes, els, null).getDecision());
    }

    @Test
    @DisplayName("Request carries intent criteria and span questions for the model")
    @SuppressWarnings("unchecked")
    void testRequestHasFullQuestionSet() {
        Map<String, Object> req = JevClient.buildRequest("search for alan turing", Collections.emptyList(), "https://example.com", "Example", "example_com", Collections.emptyMap());
        Map<String, Object> questions = (Map<String, Object>) req.get("questions");
        Map<String, Object> intentCriteria = (Map<String, Object>) ((Map<String, Object>) questions.get("intent")).get("criteria");
        assertTrue(intentCriteria.containsKey("search_web"));
        assertTrue(intentCriteria.containsKey("youtube_action"));
        assertTrue(questions.containsKey("text_span"));
        assertTrue(questions.containsKey("tab_direction"));
        assertFalse(questions.containsKey("is_correction"), "no previous action to correct");
    }
}
