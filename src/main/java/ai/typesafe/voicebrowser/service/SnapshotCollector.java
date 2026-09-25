package ai.typesafe.voicebrowser.service;

import ai.typesafe.voicebrowser.model.Constants;
import ai.typesafe.voicebrowser.model.ElementSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SnapshotCollector {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<ElementSnapshot> compactElements(List<ElementSnapshot> elements) {
        return compactElements(elements, Constants.MAX_STATE_CHARS);
    }

    public static List<ElementSnapshot> compactElements(List<ElementSnapshot> elements, int maxChars) {
        if (elements == null || elements.isEmpty()) {
            return Collections.emptyList();
        }

        // Viewport visible first, then by top position
        List<ElementSnapshot> sorted = new ArrayList<>(elements);
        sorted.sort((a, b) -> {
            boolean aV = Boolean.TRUE.equals(a.getInViewport());
            boolean bV = Boolean.TRUE.equals(b.getInViewport());
            if (aV != bV) return aV ? -1 : 1;
            int aTop = a.getTop() != null ? a.getTop() : Integer.MAX_VALUE;
            int bTop = b.getTop() != null ? b.getTop() : Integer.MAX_VALUE;
            return Integer.compare(aTop, bTop);
        });

        List<ElementSnapshot> compacted = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ElementSnapshot el : sorted) {
            String role = el.getRole() != null ? el.getRole() : "";
            String text = el.getText() != null ? el.getText().trim() : "";
            String href = el.getHref() != null ? el.getHref().trim() : "";
            String placeholder = el.getPlaceholder() != null ? el.getPlaceholder().trim() : "";

            // Omit nameless non-input elements
            boolean isInput = Set.of("textbox", "combobox", "searchbox", "input", "select").contains(role.toLowerCase());
            if (text.isEmpty() && placeholder.isEmpty() && href.isEmpty() && !isInput) {
                continue;
            }

            // Deduplicate (role, text, href)
            String key = role + "|" + text + "|" + href;
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);

            ElementSnapshot c = new ElementSnapshot();
            c.setId(el.getId());
            c.setRole(role);
            c.setTag(el.getTag());

            // Truncate text to MAX_ELEMENT_TEXT
            if (text.length() > Constants.MAX_ELEMENT_TEXT) {
                c.setText(text.substring(0, Constants.MAX_ELEMENT_TEXT));
            } else {
                c.setText(text);
            }

            if (!placeholder.isEmpty()) {
                c.setPlaceholder(placeholder.length() > Constants.MAX_ELEMENT_TEXT ?
                        placeholder.substring(0, Constants.MAX_ELEMENT_TEXT) : placeholder);
            }
            if (!href.isEmpty()) c.setHref(href);
            if (!Boolean.TRUE.equals(el.getInViewport())) {
                c.setBelowFold(true);
            }

            compacted.add(c);
            if (compacted.size() >= Constants.MAX_ELEMENTS) {
                break;
            }
        }

        // State size budget guard
        while (compacted.size() > 5) {
            try {
                String json = mapper.writeValueAsString(compacted);
                if (json.length() <= maxChars) {
                    break;
                }
                compacted.remove(compacted.size() - 1);
            } catch (Exception e) {
                break;
            }
        }

        return compacted;
    }

    public static String detectSite(String urlStr) {
        if (urlStr == null || urlStr.isEmpty() || "about:blank".equalsIgnoreCase(urlStr)) {
            return "blank";
        }
        try {
            URI uri = new URI(urlStr);
            String host = uri.getHost();
            if (host == null) return "generic";
            host = host.toLowerCase().replaceFirst("^www\\.", "");

            if (host.contains("wikipedia.org")) return "wikipedia";
            if (host.contains("news.ycombinator.com")) return "hacker_news";
            if (host.contains("google.com")) return "google";
            if (host.contains("duckduckgo.com")) return "duckduckgo";
            if (host.contains("youtube.com")) return "youtube";
            if (host.contains("github.com")) return "github";
            if (host.contains("amazon.com")) return "amazon";
            if (host.contains("reddit.com")) return "reddit";
            if (host.contains("twitter.com") || host.contains("x.com")) return "twitter_x";
            if (host.contains("example.com")) return "example_com";
        } catch (Exception e) {
            return "generic";
        }
        return "generic";
    }

    public static String findSearchBox(List<ElementSnapshot> elements) {
        if (elements == null) return null;
        for (ElementSnapshot el : elements) {
            String role = el.getRole() != null ? el.getRole().toLowerCase() : "";
            String name = el.getInputName() != null ? el.getInputName().toLowerCase() : "";
            String ph = el.getPlaceholder() != null ? el.getPlaceholder().toLowerCase() : "";

            if ("searchbox".equals(role)) {
                return el.getId();
            }
            if (Set.of("textbox", "combobox", "input").contains(role)) {
                if (name.contains("search") || ph.contains("search") || name.contains("query") || ph.contains("query")) {
                    return el.getId();
                }
            }
        }
        return null;
    }

    public static int approxTokens(Object state) {
        try {
            String json = mapper.writeValueAsString(state);
            return (int) Math.round(json.length() / 3.8);
        } catch (Exception e) {
            return 0;
        }
    }
}
