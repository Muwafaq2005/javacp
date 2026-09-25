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
            String href = el.getHref().trim();
            String host = "";
            if (href.startsWith("http://") || href.startsWith("https://")) {
                try {
                    URI uri = new URI(href);
                    if (uri.getHost() != null) {
                        host = uri.getHost().replaceFirst("^www\\.", "");
                    }
                } catch (Exception ignored) {}
            } else {
                String[] parts = href.split("/");
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    host = parts[0].replaceFirst("^www\\.", "");
                }
            }
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
            List<PolicyEngine.ActionHistoryEntry> rawActions = (List<PolicyEngine.ActionHistoryEntry>) context.get("recentActions");
            if (rawActions != null && !rawActions.isEmpty()) {
                List<PolicyEngine.ActionHistoryEntry> actions = new ArrayList<>(rawActions);
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

        // 1. Try TypeSafe AI Cloud API if API key is present
        if (hasApiKey()) {
            try {
                String apiKey = System.getenv("TYPESAFE_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) apiKey = System.getenv("JEV_API_KEY");

                Map<String, Object> body = Map.of("state", state, "questions", questions, "model", "jev-1.13.0");
                String jsonBody = mapper.writeValueAsString(body);

                HttpRequest httpReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.typesafe.ai/v1/evaluate"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(6))
                        .build();

                HttpResponse<String> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
                long latencyMs = System.currentTimeMillis() - t0;

                if (response.statusCode() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> respMap = mapper.readValue(response.body(), Map.class);
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Object>> answers = (Map<String, Map<String, Object>>) respMap.get("answers");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> usage = (Map<String, Object>) respMap.getOrDefault("usage", Map.of("input_tokens", 0, "output_tokens", 0));
                    String model = (String) respMap.getOrDefault("model", "jev-1.13.0");
                    return new JevDecisionResponse(answers, latencyMs, usage, 0.0, model, "jev-" + System.currentTimeMillis(), candidates, state, questions.size());
                }
            } catch (Exception ignored) {}
        }

        // 2. Try Local Laya FastAPI Server (http://127.0.0.1:8000/v1/evaluate)
        try {
            String layaUrl = System.getenv().getOrDefault("LAYA_SERVER_URL", "http://127.0.0.1:8000/v1/evaluate");
            Map<String, Object> body = Map.of("state", state, "questions", questions);
            String jsonBody = mapper.writeValueAsString(body);

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(layaUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(4))
                    .build();

            HttpResponse<String> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - t0;

            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> respMap = mapper.readValue(response.body(), Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> answers = (Map<String, Map<String, Object>>) respMap.get("answers");
                @SuppressWarnings("unchecked")
                Map<String, Object> usage = (Map<String, Object>) respMap.getOrDefault("usage", Map.of("input_tokens", 0, "output_tokens", 0));
                String model = (String) respMap.getOrDefault("model", "laya-local");

                return new JevDecisionResponse(answers, latencyMs, usage, 0.0, model, "laya-" + System.currentTimeMillis(), candidates, state, questions.size());
            }
        } catch (Exception e) {
            // Local Laya server offline - fall back to offline heuristic decision engine
        }

        // 3. Fallback Heuristic Evaluator for instant zero-dependency execution
        long latencyMs = System.currentTimeMillis() - t0;
        Map<String, Map<String, Object>> fallbackAnswers = buildHeuristicAnswers(transcript, elements, candidates);
        return new JevDecisionResponse(fallbackAnswers, latencyMs, Map.of("input_tokens", 0, "output_tokens", 0), 0.0, "laya-offline-fallback", "offline-" + System.currentTimeMillis(), candidates, state, questions.size());
    }

    private static Map<String, Map<String, Object>> buildHeuristicAnswers(String transcript, List<ElementSnapshot> elements, Map<String, List<String>> candidates) {
        String t = transcript.toLowerCase().trim();
        Map<String, Map<String, Object>> ans = new HashMap<>();

        // Detect Site
        String chosenSite = "none";
        for (String s : List.of("youtube", "google", "wikipedia", "github", "reddit", "twitter", "duckduckgo", "amazon")) {
            if (t.contains(s)) {
                chosenSite = s;
                break;
            }
        }
        ans.put("site", Map.of("type", "choice", "choice", chosenSite, "probabilities", Map.of(chosenSite, 1.0), "confidence", 1.0, "answer_confidence", 1.0));

        // Detect Intent
        String intent = "none";
        if (t.startsWith("open ") || t.startsWith("go to ") || t.startsWith("visit ") || t.startsWith("navigate ") || !chosenSite.equals("none")) {
            intent = "navigate_url";
        } else if (t.startsWith("search ") || t.startsWith("find ") || t.startsWith("look up ")) {
            intent = "fill_search";
        } else if (t.startsWith("click ") || t.startsWith("select ") || t.startsWith("press ") || t.startsWith("pick ")) {
            intent = "click";
        } else if (t.startsWith("scroll ")) {
            intent = "scroll";
        } else if (t.equals("go back") || t.equals("back") || t.equals("undo")) {
            intent = "back";
        }
        ans.put("intent", Map.of("type", "choice", "choice", intent, "probabilities", Map.of(intent, 0.98), "confidence", 0.98, "answer_confidence", 0.98));

        // Detect Target Element
        String targetChoice = "none";
        if (intent.equals("click") && elements != null) {
            for (ElementSnapshot el : elements) {
                if (el.getText() != null && !el.getText().isEmpty() && t.contains(el.getText().toLowerCase())) {
                    targetChoice = el.getId();
                    break;
                }
            }
            if (targetChoice.equals("none") && !elements.isEmpty()) {
                targetChoice = elements.get(0).getId();
            }
        }
        ans.put("target", Map.of("type", "choice", "choice", targetChoice, "probabilities", Map.of(targetChoice, 0.95), "confidence", 0.95, "answer_confidence", 0.95));

        // Detect Spans
        List<String> urlCands = candidates.getOrDefault("url", List.of());
        String urlChoice = urlCands.isEmpty() ? "none" : urlCands.get(0);
        ans.put("url_span", Map.of("type", "choice", "choice", urlChoice, "probabilities", Map.of(urlChoice, 1.0), "confidence", 1.0, "answer_confidence", 1.0));

        List<String> textCands = candidates.getOrDefault("text", List.of());
        String textChoice = textCands.isEmpty() ? "none" : textCands.get(0);
        ans.put("text_span", Map.of("type", "choice", "choice", textChoice, "probabilities", Map.of(textChoice, 1.0), "confidence", 1.0, "answer_confidence", 1.0));

        // Standard Signals
        ans.put("is_command", Map.of("type", "noul", "noul", 1.0, "confidence", 1.0));
        ans.put("complete", Map.of("type", "noul", "noul", 1.0, "confidence", 1.0));
        ans.put("destructive", Map.of("type", "noul", "noul", 0.0, "confidence", 1.0));
        ans.put("scroll_amount", Map.of("type", "score", "score", 1.0, "confidence", 1.0));

        return ans;
    }
}
