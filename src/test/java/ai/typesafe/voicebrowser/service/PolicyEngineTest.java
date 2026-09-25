package ai.typesafe.voicebrowser.service;

import ai.typesafe.voicebrowser.model.Action;
import ai.typesafe.voicebrowser.model.ElementSnapshot;
import ai.typesafe.voicebrowser.model.PolicyResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PolicyEngineTest {

    @Test
    public void testScrollDownPolicy() {
        Map<String, Map<String, Object>> answers = new HashMap<>();
        answers.put("intent", Map.of("choice", "scroll_down", "confidence", 0.9));
        answers.put("is_command", Map.of("noul", 0.9));
        answers.put("complete", Map.of("noul", 0.95));
        answers.put("scroll_amount", Map.of("score", 0.2));

        PolicyResult res = PolicyEngine.evaluatePolicy(
                answers,
                Collections.emptyMap(),
                Collections.emptyList(),
                "https://example.com/",
                "Example",
                "generic",
                null,
                0,
                true,
                null,
                Collections.emptyList()
        );

        assertEquals("act", res.getDecision());
        assertNotNull(res.getAction());
        assertEquals("scroll_down", res.getAction().getType());
        assertEquals("little", res.getAction().getAmount());
    }

    @Test
    public void testCorrectionReversesLastAction() {
        PolicyEngine.ActionHistoryEntry lastAction = new PolicyEngine.ActionHistoryEntry(
                "click_element", "e01", "link Documentation", null, "https://docs.typesafe.ai", "click docs", true, "done", System.currentTimeMillis() - 2000
        );

        Map<String, Map<String, Object>> answers = new HashMap<>();
        answers.put("intent", Map.of("choice", "none", "confidence", 0.1));
        answers.put("is_command", Map.of("noul", 0.9));
        answers.put("is_correction", Map.of("noul", 0.95));
        answers.put("complete", Map.of("noul", 0.95));

        PolicyResult res = PolicyEngine.evaluatePolicy(
                answers,
                Collections.emptyMap(),
                Collections.emptyList(),
                "https://docs.typesafe.ai",
                "Docs",
                "generic",
                null,
                0,
                true,
                null,
                List.of(lastAction)
        );

        assertEquals("act", res.getDecision());
        assertNotNull(res.getAction());
        assertEquals("go_back", res.getAction().getType());
    }

    @Test
    public void testDestructiveActionRequiresConfirmation() {
        ElementSnapshot el = new ElementSnapshot("e05", "button", "Delete account");

        Map<String, Map<String, Object>> answers = new HashMap<>();
        answers.put("intent", Map.of("choice", "click_element", "confidence", 0.95));
        answers.put("target", Map.of("choice", "e05", "confidence", 0.95, "probabilities", Map.of("e05", 0.95)));
        answers.put("is_command", Map.of("noul", 0.95));
        answers.put("complete", Map.of("noul", 0.95));
        answers.put("destructive", Map.of("noul", 0.85));

        PolicyResult res = PolicyEngine.evaluatePolicy(
                answers,
                Collections.emptyMap(),
                List.of(el),
                "https://shop.example.com/account",
                "Account",
                "generic",
                null,
                0,
                true,
                null,
                Collections.emptyList()
        );

        assertEquals("confirm", res.getDecision());
        assertNotNull(res.getAction());
        assertEquals("click_element", res.getAction().getType());
    }
}
