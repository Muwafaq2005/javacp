package ai.typesafe.voicebrowser.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.typesafe.voicebrowser.model.Constants;
import ai.typesafe.voicebrowser.model.ElementSnapshot;

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
            int questionCount,
            String warning // why a higher tier was skipped (API error, Laya offline), null when none
    ) {}

    private static Map<String, Object> crit(String what, String notFor, String... examples) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("what", what);
        if (notFor != null) c.put("not_for", notFor);
        if (examples.length > 0) c.put("examples", List.of(examples));
        return c;
    }

    private static Map<String, Object> question(String type, String question, String focus, Object criteria) {
        Map<String, Object> instructions = new LinkedHashMap<>();
        instructions.put("question", question);
        if (focus != null) instructions.put("focus", focus);
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("type", type);
        q.put("instructions", instructions);
        if (criteria != null) q.put("criteria", criteria);
        return q;
    }

    private static Map<String, Object> noulCriteria(Map<String, Object> yes, Map<String, Object> no) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("true", yes);
        c.put("false", no);
        return c;
    }

    // Every intent PolicyEngine/Executor understands. Without these criteria the model has no options to choose from.
    private static final Map<String, Object> INTENT_CRITERIA = new LinkedHashMap<>();
    static {
        INTENT_CRITERIA.put("navigate_url", crit("Open a specific website or URL by name (go to / open / visit / take me to <site>)", "Searching for a topic; clicking something already on the page", "go to wikipedia", "open youtube", "take me to github.com", "visit example dot com"));
        INTENT_CRITERIA.put("search_web", crit("Search for a topic or phrase (search for / look up / google / find <query>), on the web or on a named site", "Typing into a specific named field without searching; opening a site's homepage", "search for alan turing", "look up typesafe jev", "search wikipedia for cats"));
        INTENT_CRITERIA.put("click_element", crit("Click / press / open / select / choose a link, button, tab, result or item that is on the current page, named by its text", "Opening a website by name; typing text; picking by position number (that is select_ordinal)", "click sign in", "press the more information link", "open the comments tab"));
        INTENT_CRITERIA.put("select_ordinal", crit("Open the Nth result, video, link or item by its position", "Clicking an element named by its text", "click the first result", "open the second video", "play the 3rd one"));
        INTENT_CRITERIA.put("type_into_field", crit("Type or enter specific text into an input box, search box or text field on the page", "Running a search on a search engine (that is search_web); pressing enter alone", "type hello world into the search box", "write good morning in the comment box"));
        INTENT_CRITERIA.put("select_option", crit("Choose an option from a dropdown / select menu", "Clicking a link or button", "select english from the language dropdown", "choose the large size"));
        INTENT_CRITERIA.put("clear_field", crit("Clear / empty one input field", "Clearing every field of a form", "clear the search box", "empty that field"));
        INTENT_CRITERIA.put("clear_form", crit("Clear every field of the form on the page", "Clearing one field", "clear form", "reset the form"));
        INTENT_CRITERIA.put("press_enter", crit("Press the Enter / Return key, or submit what was typed", "Typing text; clicking a named button", "press enter", "hit enter", "submit"));
        INTENT_CRITERIA.put("press_key", crit("Press a named keyboard key other than Enter (escape, space, tab)", "Pressing enter / submitting", "press escape", "press space", "hit tab"));
        INTENT_CRITERIA.put("scroll_down", crit("Scroll / move down the page", "Scrolling up; jumping to the very bottom", "scroll down", "scroll down a bit", "page down"));
        INTENT_CRITERIA.put("scroll_up", crit("Scroll / move up the page", "Scrolling down; jumping to the very top", "scroll up", "page up"));
        INTENT_CRITERIA.put("scroll_top", crit("Jump to the very top of the page", "Scrolling up a little", "scroll to the top", "back to the top"));
        INTENT_CRITERIA.put("scroll_bottom", crit("Jump to the very bottom of the page", "Scrolling down a little", "scroll to the bottom", "go to the bottom"));
        INTENT_CRITERIA.put("go_back", crit("Go back to the previous page in history (back / go back / undo that / previous page)", "Scrolling up; closing a tab", "go back", "undo", "previous page"));
        INTENT_CRITERIA.put("go_forward", crit("Go forward in history", "Scrolling down", "go forward", "forward"));
        INTENT_CRITERIA.put("reload", crit("Reload / refresh the current page", "Navigating elsewhere", "reload", "refresh the page"));
        INTENT_CRITERIA.put("open_new_tab", crit("Open a new empty tab", "Opening a website by name in the current tab", "open a new tab", "new tab"));
        INTENT_CRITERIA.put("close_tab", crit("Close the current tab", "Going back", "close this tab", "close tab"));
        INTENT_CRITERIA.put("switch_tab", crit("Switch to another / the next / the previous tab", "Opening or closing tabs", "next tab", "switch tab", "go to the other tab"));
        INTENT_CRITERIA.put("zoom_page", crit("Zoom the page in or out, or reset zoom", null, "zoom in", "zoom out", "zoom to 150%", "reset zoom"));
        INTENT_CRITERIA.put("read_content", crit("Read out the main text of the page", null, "read the page", "read this article"));
        INTENT_CRITERIA.put("media_control", crit("Play, pause, resume or stop the video / audio on the page", "Changing volume, speed or position", "play", "pause", "resume the video"));
        INTENT_CRITERIA.put("volume_control", crit("Change the volume of the video / audio", null, "volume up", "turn the volume down"));
        INTENT_CRITERIA.put("seek_media", crit("Skip forward or rewind the video / audio by an amount", null, "skip 10 seconds", "rewind 30 seconds", "fast forward"));
        INTENT_CRITERIA.put("playback_speed", crit("Change the playback speed of the video", "Searching for something containing the word speed", "speed 1.5x", "play faster", "normal speed"));
        INTENT_CRITERIA.put("youtube_action", crit("A YouTube player action: captions, theater mode, miniplayer, like, subscribe, jump to comments, next / previous chapter", "Searching YouTube; clicking a named element", "turn on captions", "theater mode", "like this video", "next chapter"));
        INTENT_CRITERIA.put("confirm", crit("Approve a pending action the browser asked to confirm (yes / confirm / do it / go ahead)", "New commands", "confirm", "yes do it", "go ahead"));
        INTENT_CRITERIA.put("cancel", crit("Cancel / never mind / stop the pending action", "Going back in history", "cancel", "never mind"));
        INTENT_CRITERIA.put("none", crit("Not a browser command, or nothing recognizable yet (fragment, chit-chat, silence, filler)", "Anything that clearly matches another option", "um", "okay so", "the weather is nice"));
    }

    private static final Map<String, Object> SITE_CRITERIA = new LinkedHashMap<>();
    static {
        SITE_CRITERIA.put("google", "Google (google, google it)");
        SITE_CRITERIA.put("duckduckgo", "DuckDuckGo");
        SITE_CRITERIA.put("the_web", "A general web search with no site named (search the web, look it up online)");
        SITE_CRITERIA.put("youtube", "YouTube (videos)");
        SITE_CRITERIA.put("wikipedia", "Wikipedia (the encyclopedia)");
        SITE_CRITERIA.put("github", "GitHub (code, repositories)");
        SITE_CRITERIA.put("amazon", "Amazon (shopping)");
        SITE_CRITERIA.put("reddit", "Reddit");
        SITE_CRITERIA.put("twitter_x", "Twitter / X");
        SITE_CRITERIA.put("hacker_news", "Hacker News (news.ycombinator.com, hn)");
        SITE_CRITERIA.put("example_com", "example.com / example dot com");
        SITE_CRITERIA.put("other_named_site", "Some other website named explicitly in `transcript` (a domain or brand not listed above)");
        SITE_CRITERIA.put("none", "No website or search engine is mentioned in `transcript`");
    }

    private static final Map<String, Object> TAB_DIRECTION_CRITERIA = new LinkedHashMap<>();
    static {
        TAB_DIRECTION_CRITERIA.put("next", "The next tab / the other tab / switch tab with no direction");
        TAB_DIRECTION_CRITERIA.put("previous", "The previous tab / the tab before / last tab");
        TAB_DIRECTION_CRITERIA.put("first", "The first tab");
        TAB_DIRECTION_CRITERIA.put("none", "Not about switching tabs");
    }

    public static double costUsd(Map<String, Object> usage) {
        Object tokens = usage != null ? usage.get("input_tokens") : null;
        return tokens instanceof Number n ? n.doubleValue() / 1_000_000 * Constants.PRICE_PER_M_INPUT_TOKENS_USD : 0.0;
    }

    public static boolean hasApiKey() {
        String key = System.getenv("OPENCODE_API_KEY");
        return key != null && !key.isBlank();
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

        Map<String, Object> questions = new LinkedHashMap<>();

        Map<String, Object> targetCriteria = new LinkedHashMap<>();
        if (elements != null) {
            for (ElementSnapshot el : elements) {
                targetCriteria.put(el.getId(), null);
            }
        }
        targetCriteria.put("none", "No element on this page is referred to");

        questions.put("intent", question("choice",
                "Which browser action does the user ask for in `transcript`?",
                "Judge the words said so far. If the sentence is unfinished, pick the action the words already commit to; if no action is recognizable pick none. `page` and `elements` describe what is currently on screen. `context.previous_page` and `context.recent_actions` (most recent first) say where the user just came from and what was just done: 'back to the results' after clicking a search result is go_back; 'the other one' or 'not that one' after a click is click_element on a different element.",
                INTENT_CRITERIA));
        questions.put("target", question("choice",
                "Which element in `elements` is the one the user refers to in `transcript` (the thing to click, type into or select)? Each line of `elements` starts with the element id (e.g. e07), then its role and visible text; the options are those ids.",
                "Match by the element's visible text, role and position words like first/second/top (lines are in visual order, top of page first). Use `context.recent_actions` for relative references: 'the other one' / 'not that one' mean an element other than the target of the most recent action. Pick none if the command does not refer to any element on this page, or if the referenced element is not in the list.",
                targetCriteria));
        questions.put("site", question("choice",
                "Which website or search engine does the user name in `transcript`?",
                "Only what is explicitly said. Pick none if no site is named.",
                SITE_CRITERIA));
        questions.put("complete", question("noul",
                "Has the user finished saying the command in `transcript`, so it can be executed now without waiting for more words?",
                "Speech arrives word by word. A command is complete when its verb and any required object are present (a site for go to, a query for search for, an element for click, text for type).",
                noulCriteria(
                        crit("Complete, actionable command", null, "scroll down", "go back", "go to wikipedia", "search for alan turing", "click the first result"),
                        crit("Cut off before the required object; more words are clearly coming", null, "go to", "search for", "click the", "type", "open the", "scroll"))));
        questions.put("is_command", question("noul",
                "Is `transcript` an instruction addressed to a web browser (navigate, search, click, type, scroll, tabs, media, confirm/cancel)?",
                "Chit-chat, narration, talking to another person, or a stray fragment is not a command. A reaction to what the browser just did in `context.recent_actions` ('no, not that one', 'undo that', 'wrong link', 'yes confirm') IS addressed to the browser.",
                noulCriteria(
                        crit("An imperative aimed at the browser, or a correction / confirmation of its last action", null, "scroll down", "go to youtube", "click sign in", "no not that one", "undo that", "confirm"),
                        crit("Not directed at the browser", null, "I think we should get lunch", "um so yeah", "this is the demo", "what did you say"))));
        questions.put("destructive", question("noul",
                "Would carrying out the action in `transcript` on this `page` submit a form, place an order, pay, delete, send a message, post publicly, log out, or otherwise do something hard to undo?",
                "Navigating, scrolling, reading, clicking links and typing into a box are NOT destructive.",
                noulCriteria(
                        crit("Irreversible side effect", null, "click buy now", "delete this repository", "send the message", "post the comment", "click checkout"),
                        crit("Reversible / read-only", null, "scroll down", "go to wikipedia", "click the first result", "type hello in the search box"))));
        questions.put("scroll_amount", question("score",
                "How far does the user want to scroll according to `transcript`?",
                "Only relevant when scrolling; default is one screen when nothing is specified.",
                List.of(
                        Map.of("what", "A little: a few lines (a bit, slightly, a little)"),
                        Map.of("what", "One screen / one page, or no amount specified"),
                        Map.of("what", "All the way to the end: the very top or the very bottom"))));
        questions.put("tab_direction", question("choice",
                "When switching tabs, which tab does `transcript` refer to?", null, TAB_DIRECTION_CRITERIA));

        // Only asked when there is a previous action to correct
        if (ctx != null && ctx.containsKey("recent_actions")) {
            questions.put("is_correction", question("noul",
                    "Is the user in `transcript` saying that the most recent action in `context.recent_actions` was wrong and should be reversed or redirected?",
                    "A correction reacts to what just happened ('no', 'not that one', 'wrong link', 'undo that', 'the other one', 'I meant the second one'). A fresh command that merely follows the previous action is not a correction.",
                    noulCriteria(
                            crit("Rejects or redirects the previous action", null, "no not that one", "wrong one, go back", "undo that", "I meant the other link"),
                            crit("A new command or a continuation, satisfied with the previous action", null, "scroll down", "now click the comments", "open the documentation", "search for cats"))));
        }

        // Span questions: the model only picks one of the code-extracted candidates, which is then copied verbatim
        if (!textCandidates.isEmpty()) {
            Map<String, Object> c = new LinkedHashMap<>();
            for (String s : textCandidates) c.put(s, null);
            c.put("none", "Nothing should be typed or searched");
            questions.put("text_span", question("choice",
                    "Which option is exactly the text the user wants typed or searched, as spoken in `transcript`? Options are verbatim candidate spans.",
                    "Choose the span that contains the payload text only, without the command words (type, search for, into the search box). Pick none if nothing should be typed.",
                    c));
        }
        if (!urlCandidates.isEmpty()) {
            Map<String, Object> c = new LinkedHashMap<>();
            for (String s : urlCandidates) c.put(s, null);
            c.put("none", "No web address is mentioned");
            questions.put("url_span", question("choice",
                    "Which option is the web address (domain) the user wants to open, as spoken in `transcript`?",
                    "Pick none if no address is mentioned.",
                    c));
        }

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
        List<String> warnings = new ArrayList<>();

        // 1. Try OpenCode's free Jev model
        if (hasApiKey()) {
            try {
                String apiKey = System.getenv("OPENCODE_API_KEY");

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("state", state);
                body.put("questions", questions);
                body.put("model", "jev-1.13-free");

                String jsonBody = mapper.writeValueAsString(body);

                HttpRequest httpReq = HttpRequest.newBuilder()
                        .uri(URI.create(
                            "https://opencode.ai/inference/systemone/v1/systemone"
                        ))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        httpReq,
                        HttpResponse.BodyHandlers.ofString()
                );

                long latencyMs = System.currentTimeMillis() - t0;

                if (response.statusCode() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> respMap =
                            mapper.readValue(response.body(), Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Object>> answers =
                            (Map<String, Map<String, Object>>)
                                    respMap.get("answers");

                    @SuppressWarnings("unchecked")
                    Map<String, Object> usage =
                            (Map<String, Object>) respMap.getOrDefault(
                                    "usage", Map.of()
                            );

                    double cost = 0.0;
                    Object costValue = usage.get("cost");
                    if (costValue instanceof Number n) {
                        cost = n.doubleValue();
                    }

                    String model = String.valueOf(
                            respMap.getOrDefault("model", "jev-1.13-free")
                    );

                    return new JevDecisionResponse(
                            answers,
                            latencyMs,
                            usage,
                            cost,
                            model,
                            "opencode-" + System.currentTimeMillis(),
                            candidates,
                            state,
                            questions.size(),
                            null
                    );
                }

                warnings.add("OpenCode HTTP " + response.statusCode()
                        + ": " + abbreviate(response.body()));

            } catch (Exception e) {
                warnings.add("OpenCode API error: " + e.getMessage());
            }
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

                return new JevDecisionResponse(answers, latencyMs, usage, 0.0, model, "laya-" + System.currentTimeMillis(), candidates, state, questions.size(), joinWarnings(warnings));
            }
            warnings.add("Laya HTTP " + response.statusCode() + ": " + abbreviate(response.body()));
        } catch (Exception e) {
            // Local Laya server offline - fall back to offline heuristic decision engine.
            // Only worth reporting when the cloud API was also expected to answer.
            if (!warnings.isEmpty()) warnings.add("Laya offline");
        }

        // 3. Fallback Heuristic Evaluator for instant zero-dependency execution
        long latencyMs = System.currentTimeMillis() - t0;
        Map<String, Map<String, Object>> fallbackAnswers = buildHeuristicAnswers(transcript, elements, candidates);
        return new JevDecisionResponse(fallbackAnswers, latencyMs, Map.of("input_tokens", 0, "output_tokens", 0), 0.0, "laya-offline-fallback", "offline-" + System.currentTimeMillis(), candidates, state, questions.size(), joinWarnings(warnings));
    }

    private static String joinWarnings(List<String> warnings) {
        return warnings.isEmpty() ? null : String.join("; ", warnings) + " — using fallback";
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }

    private static final java.util.regex.Pattern DESTRUCTIVE_WORDS = java.util.regex.Pattern.compile(
            "\\b(buy|purchase|checkout|check out|place order|pay|delete|remove|send|post|publish|submit|log ?out|sign ?out|unsubscribe)\\b");

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
        String bare = t.replaceAll("[.,!?]", "");
        if (bare.matches("(confirm|yes|yes do it|do it|go ahead)")) {
            intent = "confirm";
        } else if (bare.matches("(cancel|never ?mind|no|don't)")) {
            intent = "cancel";
        } else if (t.startsWith("zoom") || t.contains("zoom in") || t.contains("zoom out") || t.contains("reset zoom")) {
            intent = "zoom_page";
        } else if (t.contains("read page") || t.contains("read article") || t.contains("read content") || t.contains("read main")) {
            intent = "read_content";
        } else if (t.equals("clear form") || t.equals("reset form") || t.equals("empty form")) {
            intent = "clear_form";
        } else if (t.matches(".*(speed|rate|playback speed).*") || t.contains("faster") || t.contains("slower") || t.contains("double speed") || t.contains("normal speed")) {
            intent = "playback_speed";
        } else if (t.contains("subtitles") || t.contains("captions") || t.equals("cc") || t.contains("toggle cc") || t.contains("theater mode") || t.contains("theatre mode") || t.contains("miniplayer") || t.contains("like video") || t.equals("like") || t.contains("subscribe") || t.contains("comments") || t.contains("next chapter") || t.contains("previous chapter")) {
            intent = "youtube_action";
        } else if (t.matches(".*(volume|sound)\\s*(up|down|increase|decrease|more|less).*") || t.matches(".*set\\s*volume.*")) {
            intent = "volume_control";
        } else if (t.matches(".*(skip|rewind|forward|backward|seek).*\\s*\\d+.*") || t.matches(".*(fast forward|rewind).*")) {
            intent = "seek_media";
        } else if (t.equals("play") || t.equals("pause") || t.equals("stop") || t.equals("resume") || t.equals("mute") || t.equals("unmute") || t.contains("fullscreen")) {
            intent = "media_control";
        } else if (t.matches(".*(first|second|third|fourth|fifth|1st|2nd|3rd|4th|5th|item|option|video|link|result).*") &&
                   (t.contains("click") || t.contains("open") || t.contains("select") || t.contains("choose") || t.contains("pick") || t.matches("^(the\\s+)?(first|second|third|fourth|fifth|1st|2nd|3rd|4th|5th).*"))) {
            intent = "select_ordinal";
        } else if (t.startsWith("press ") && (t.contains("enter") || t.contains("escape") || t.contains("space") || t.contains("tab"))) {
            intent = "press_key";
        } else if (t.equals("new tab") || t.equals("open new tab") || (t.startsWith("open ") && t.endsWith("in new tab"))) {
            intent = "open_new_tab";
        } else if (t.equals("close tab") || t.equals("close current tab")) {
            intent = "close_tab";
        } else if (t.contains("next tab") || t.contains("previous tab") || t.startsWith("switch tab") || t.startsWith("switch to tab")) {
            intent = "switch_tab";
        } else if (t.equals("scroll to top") || t.equals("scroll top")) {
            intent = "scroll_top";
        } else if (t.equals("scroll to bottom") || t.equals("scroll bottom")) {
            intent = "scroll_bottom";
        } else if (t.startsWith("clear") || t.startsWith("empty ")) {
            intent = "clear_field";
        } else if (t.equals("reload") || t.equals("refresh")) {
            intent = "reload";
        } else if (t.equals("go forward") || t.equals("forward")) {
            intent = "go_forward";
        } else if (t.startsWith("search ") || t.startsWith("find ") || t.startsWith("look up ") || t.contains("search for ") || t.matches(".*search\\s+.*(in|on)\\s+.*")) {
            intent = "search_web";
        } else if (t.startsWith("open ") || t.startsWith("go to ") || t.startsWith("visit ") || t.startsWith("navigate ") || !chosenSite.equals("none")) {
            intent = "navigate_url";
        } else if (t.startsWith("click ") || t.startsWith("select ") || t.startsWith("press ") || t.startsWith("pick ")) {
            intent = "click_element";
        } else if (t.startsWith("scroll up")) {
            intent = "scroll_up";
        } else if (t.startsWith("scroll")) {
            intent = "scroll_down";
        } else if (t.equals("go back") || t.equals("back") || t.equals("undo")) {
            intent = "go_back";
        }
        ans.put("intent", Map.of("type", "choice", "choice", intent, "probabilities", Map.of(intent, 0.98), "confidence", 0.98, "answer_confidence", 0.98));

        // Detect Target Element
        String targetChoice = "none";
        if ((intent.equals("click_element") || intent.equals("clear_field")) && elements != null) {
            for (ElementSnapshot el : elements) {
                if (el.getText() != null && !el.getText().isEmpty() && t.contains(el.getText().toLowerCase())) {
                    targetChoice = el.getId();
                    break;
                }
            }
            // No text match: leave target "none" so the policy waits instead of clicking an arbitrary element
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
        boolean destructive = DESTRUCTIVE_WORDS.matcher(t).find();
        ans.put("destructive", Map.of("type", "noul", "noul", destructive ? 0.9 : 0.0, "confidence", 1.0));
        ans.put("scroll_amount", Map.of("type", "score", "score", 1.0, "confidence", 1.0));

        return ans;
    }
}
