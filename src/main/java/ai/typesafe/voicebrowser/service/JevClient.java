package ai.typesafe.voicebrowser.service;

import ai.typesafe.voicebrowser.model.Constants;
import ai.typesafe.voicebrowser.model.ElementSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class JevClient {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper mapper = new ObjectMapper();

    public record JevDecisionResponse(
            Map<String, Map<String, Object>> answers,
            long latencyMs,
            Map<String, Object> usage,
            double costUsd,
            String model,
            String requestId,
            Map<String, List<String>> candidates,
            Map<String, Object> state,
            int questionCount
    ) {}

    public static boolean hasApiKey() {
        String key = System.getenv("TYPESAFE_API_KEY");
        if (key == null || key.isEmpty()) key = System.getenv("JEV_API_KEY");
        return key != null && !key.isEmpty() && !"your_typesafe_api_key_here".equals(key);
    }

    public static String encodeElement(ElementSnapshot el, String pageHost) {
        StringBuilder sb = new StringBuilder();
        sb.append(el.getId()).append(" ").append(el.getRole() != null ? el.getRole() : "");
        String text = el.getText() != null ? el.getText() : "";
        if (!text.isEmpty()) {
            sb.append(" \"").append(text).append("\"");
        }
        if (el.getPlaceholder() != null && !el.getPlaceholder().isEmpty() && !el.getPlaceholder().equals(text)) {
            sb.append(" (placeholder: ").append(el.getPlaceholder()).append(")");
        }
        if (el.getHref() != null && !el.getHref().isEmpty()) {
            String host = el.getHref().split("/")[0];
            if (!host.isEmpty() && !host.equalsIgnoreCase(pageHost)) {
                sb.append(" → ").append(host);
            }
        }
        if (Boolean.TRUE.equals(el.getBelowFold())) {
            sb.append(" [below fold]");
        }
        return sb.toString();
    }

    public static Map<String, Object> encodeContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) return null;
        Map<String, Object> out = new HashMap<>();

        if (context.containsKey("previousPage")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> prev = (Map<String, Object>) context.get("previousPage");
            if (prev != null && prev.containsKey("url")) {
                Map<String, String> prevMap = new HashMap<>();
                prevMap.put("url", String.valueOf(prev.get("url")));
                prevMap.put("title", String.valueOf(prev.getOrDefault("title", "")));
                out.put("previous_page", prevMap);
            }
        }

        if (context.containsKey("recentActions")) {
            @SuppressWarnings("unchecked")
            List<PolicyEngine.ActionHistoryEntry> actions = (List<PolicyEngine.ActionHistoryEntry>) context.get("recentActions");
            if (actions != null && !actions.isEmpty()) {
                List<Map<String, Object>> recentList = new ArrayList<>();
                long now = System.currentTimeMillis();
                for (int i = Math.max(0, actions.size() - Constants.MAX_CONTEXT_ACTIONS); i < actions.size(); i++) {
                    PolicyEngine.ActionHistoryEntry a = actions.get(i);
                    Map<String, Object> e = new HashMap<>();
                    e.put("said", a.said());
                    e.put("action", a.type());
                    if (a.targetLabel() != null) e.put("target", a.targetLabel());
                    if (a.text() != null) e.put("text", a.text());
                    if (a.url() != null) e.put("url", a.url());
                    e.put("outcome", a.outcome() != null ? a.outcome() : (a.ok() ? "done" : "failed"));
                    if (a.at() > 0) e.put("seconds_ago", Math.max(0, Math.round((now - a.at()) / 1000.0)));
                    recentList.add(e);
                }
                Collections.reverse(recentList);
                out.put("recent_actions", recentList);
            }
        }
        return out.isEmpty() ? null : out;
    }

    public static Map<String, Object> buildRequest(String transcript, List<ElementSnapshot> elements, String pageUrl, String pageTitle, String site, Map<String, Object> context) {
        String text = SpansExtractor.cleanTranscript(transcript);
        if (text.length() > Constants.MAX_TRANSCRIPT_CHARS) {
            text = text.substring(text.length() - Constants.MAX_TRANSCRIPT_CHARS);
        }

        List<String> textCandidates = SpansExtractor.extractTextCandidates(text);
        List<String> urlCandidates = SpansExtractor.extractUrlCandidates(text);

        String pageHost = "";
        try {
            if (pageUrl != null && !pageUrl.isEmpty() && !"about:blank".equals(pageUrl)) {
                pageHost = new URI(pageUrl).getHost().replaceFirst("^www\\.", "");
            }
        } catch (Exception ignored) {}

        Map<String, Object> state = new HashMap<>();
        state.put("transcript", text);

        Map<String, String> page = new HashMap<>();
        page.put("url", pageUrl != null ? pageUrl : "about:blank");
        page.put("title", pageTitle != null ? pageTitle : "");
        page.put("site", site != null ? site : "blank");
        state.put("page", page);

        List<String> encodedElements = new ArrayList<>();
        if (elements != null) {
            for (ElementSnapshot el : elements) {
                encodedElements.add(encodeElement(el, pageHost));
            }
        }
        state.put("elements", encodedElements);

        Map<String, Object> ctx = encodeContext(context);
        if (ctx != null) state.put("context", ctx);

        Map<String, Object> questions = new HashMap<>();

        Map<String, Object> targetCriteria = new HashMap<>();
        if (elements != null) {
            for (ElementSnapshot el : elements) {
                targetCriteria.put(el.getId(), null);
            }
        }
        targetCriteria.put("none", "No element on this page is referred to");

        questions.put("intent", Map.of("type", "choice", "instructions", Map.of("question", "Which browser action does the user ask for in `transcript`?")));
        questions.put("target", Map.of("type", "choice", "instructions", Map.of("question", "Which element in `elements` is the one referred to?"), "criteria", targetCriteria));
        questions.put("site", Map.of("type", "choice", "instructions", Map.of("question", "Which website or search engine does the user name in `transcript`?"), "criteria", Constants.SITE_SEARCH));
        questions.put("complete", Map.of("type", "noul", "instructions", Map.of("question", "Has the user finished saying the command?")));
        questions.put("is_command", Map.of("type", "noul", "instructions", Map.of("question", "Is `transcript` an instruction addressed to a web browser?")));
        questions.put("destructive", Map.of("type", "noul", "instructions", Map.of("question", "Would carrying out the action be destructive?")));
        questions.put("scroll_amount", Map.of("type", "score", "instructions", Map.of("question", "How far does the user want to scroll?")));

        Map<String, List<String>> candidates = Map.of("text", textCandidates, "url", urlCandidates);

        Map<String, Object> req = new HashMap<>();
        req.put("state", state);
        req.put("questions", questions);
        req.put("candidates", candidates);
        return req;
    }

    public static JevDecisionResponse decide(String transcript, List<ElementSnapshot> elements, String pageUrl, String pageTitle, String site, Map<String, Object> context) {
        Map<String, Object> reqMap = buildRequest(transcript, elements, pageUrl, pageTitle, site, context);
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) reqMap.get("state");
        @SuppressWarnings("unchecked")
        Map<String, Object> questions = (Map<String, Object>) reqMap.get("questions");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> candidates = (Map<String, List<String>>) reqMap.get("candidates");

        long t0 = System.currentTimeMillis();

        try {
            String layaUrl = System.getenv().getOrDefault("LAYA_SERVER_URL", "http://127.0.0.1:8000/v1/evaluate");
            Map<String, Object> body = Map.of("state", state, "questions", questions);
            String jsonBody = mapper.writeValueAsString(body);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(layaUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<String> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - t0;

            if (response.statusCode() != 200) {
                throw new RuntimeException("Laya Server error " + response.statusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> respMap = mapper.readValue(response.body(), Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> answers = (Map<String, Map<String, Object>>) respMap.get("answers");
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) respMap.getOrDefault("usage", Map.of("input_tokens", 0, "output_tokens", 0));
            String model = (String) respMap.getOrDefault("model", "laya-local");

            return new JevDecisionResponse(answers, latencyMs, usage, 0.0, model, "laya-" + System.currentTimeMillis(), candidates, state, questions.size());

        } catch (Exception e) {
            throw new RuntimeException("Failed to reach model server: " + e.getMessage(), e);
        }
    }
}
