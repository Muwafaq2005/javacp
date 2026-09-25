package ai.typesafe.voicebrowser.model;

import java.util.*;

public class Constants {
    public static final String MODEL = "jev-1.13.0";
    public static final double PRICE_PER_M_INPUT_TOKENS_USD = 0.042;

    public static final int MAX_ELEMENTS = 100;
    public static final int MAX_ELEMENT_TEXT = 60;
    public static final int MAX_STATE_CHARS = 24000;
    public static final int MAX_TRANSCRIPT_CHARS = 400;
    public static final int MAX_CONTEXT_ACTIONS = 3;

    public static final long DEBOUNCE_MS = 200;
    public static final int MAX_INFLIGHT = 2;
    public static final long SILENCE_COMPLETE_MS = 900;
    public static final long PAYLOAD_SILENCE_MS = 600;
    public static final long HIGHLIGHT_MS = 600;
    public static final long CANDIDATE_TTL_MS = 8000;

    public record Thresholds(
            double intentConfidence,
            double complete,
            double isCommand,
            double destructive,
            double destructiveIntentConfidence,
            double targetConfidence,
            double targetTopProb,
            double spanConfidence,
            int candidateCount,
            double correction
    ) {}

    public static final Thresholds T = new Thresholds(
            0.55, // intentConfidence
            0.6,  // complete
            0.5,  // isCommand
            0.5,  // destructive
            0.9,  // destructiveIntentConfidence
            0.45, // targetConfidence
            0.35, // targetTopProb
            0.35, // spanConfidence
            3,    // candidateCount
            0.6   // correction
    );

    public static final Set<String> PAYLOAD_INTENTS = Set.of(
            "search_web", "type_into_field", "select_option"
    );

    public static final Set<String> TARGET_INTENTS = Set.of(
            "click_element", "type_into_field", "select_option"
    );

    public static final Map<String, String> SITE_HOME = Map.ofEntries(
            Map.entry("google", "https://www.google.com/"),
            Map.entry("duckduckgo", "https://duckduckgo.com/"),
            Map.entry("youtube", "https://www.youtube.com/"),
            Map.entry("wikipedia", "https://en.wikipedia.org/wiki/Main_Page"),
            Map.entry("github", "https://github.com/"),
            Map.entry("amazon", "https://www.amazon.com/"),
            Map.entry("reddit", "https://www.reddit.com/"),
            Map.entry("twitter_x", "https://x.com/"),
            Map.entry("hacker_news", "https://news.ycombinator.com/"),
            Map.entry("example_com", "https://example.com/")
    );

    public static final Map<String, String> SITE_SEARCH = Map.ofEntries(
            Map.entry("google", "https://www.google.com/search?q=%s"),
            Map.entry("duckduckgo", "https://duckduckgo.com/?q=%s"),
            Map.entry("the_web", "https://duckduckgo.com/?q=%s"),
            Map.entry("youtube", "https://www.youtube.com/results?search_query=%s"),
            Map.entry("wikipedia", "https://en.wikipedia.org/w/index.php?search=%s"),
            Map.entry("github", "https://github.com/search?q=%s&type=repositories"),
            Map.entry("amazon", "https://www.amazon.com/s?k=%s"),
            Map.entry("reddit", "https://www.reddit.com/search/?q=%s"),
            Map.entry("twitter_x", "https://x.com/search?q=%s"),
            Map.entry("hacker_news", "https://hn.algolia.com/?q=%s")
    );

    public static final String DEFAULT_SEARCH_ENGINE = "duckduckgo";
}
