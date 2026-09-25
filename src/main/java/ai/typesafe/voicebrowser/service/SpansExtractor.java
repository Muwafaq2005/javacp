package ai.typesafe.voicebrowser.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpansExtractor {

    private static final String TLDS = "com|org|net|io|ai|dev|co|edu|gov|de|uk|us|app|xyz|info|me|tv|ch|at|fr|nl|es|it";

    private static final Pattern FILLER_RE = Pattern.compile("\\b(please|thanks|thank you|now|okay|ok|um|uh|and then)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern[] TEXT_VERBS = new Pattern[]{
            Pattern.compile("\\b(?:search|look)\\s+(?:for|up)\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsearch\\s+(?:on\\s+)?(?:google|duckduckgo|wikipedia|youtube|github|amazon|reddit|twitter|x|hacker news|the web)\\s+for\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsearch\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bgoogle\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bfind\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btype\\s+(?:in\\s+)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\benter\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwrite\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bput\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bfill\\s+(?:in\\s+)?", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern TRAILING_DEST_RE = Pattern.compile(
            "\\s+(?:in|into|on|inside|to)\\s+(?:the\\s+)?(?:[\\w-]+\\s+){0,4}?(?:box|field|input|bar|form|textarea|search|wikipedia|youtube|google|duckduckgo|github|amazon|reddit|twitter|x|web)\\b.*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LEADING_SITE_RE = Pattern.compile(
            "^(?:on\\s+|in\\s+)?(?:google|duckduckgo|wikipedia|youtube|github|amazon|reddit|twitter|x|hacker news|the web)\\s+(?:for\\s+)?",
            Pattern.CASE_INSENSITIVE
    );

    public static String cleanTranscript(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String stripFiller(String s) {
        if (s == null) return "";
        return FILLER_RE.matcher(s).replaceAll(" ")
                .replaceAll("\\s+", " ")
                .replaceAll("[.,!?]+$", "")
                .trim();
    }

    private static void pushUnique(List<String> list, String value) {
        String v = stripFiller(value);
        if (v.isEmpty() || v.length() > 120) return;
        boolean exists = list.stream().anyMatch(x -> x.equalsIgnoreCase(v));
        if (!exists) {
            list.add(v);
        }
    }

    public static List<String> extractTextCandidates(String transcript) {
        String t = cleanTranscript(transcript);
        if (t.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>();

        // 1. Quoted spans
        Matcher quoteMatcher = Pattern.compile("[\"“”']([^\"“”']{1,120})[\"“”']").matcher(t);
        while (quoteMatcher.find()) {
            pushUnique(out, quoteMatcher.group(1));
        }

        // 2. Text after a payload verb (earliest verb first)
        class VerbMatch {
            int index;
            int length;
            VerbMatch(int index, int length) {
                this.index = index;
                this.length = length;
            }
        }
        List<VerbMatch> matches = new ArrayList<>();
        for (Pattern p : TEXT_VERBS) {
            Matcher m = p.matcher(t);
            if (m.find()) {
                matches.add(new VerbMatch(m.start(), m.group().length()));
            }
        }
        matches.sort((a, b) -> a.index != b.index ? Integer.compare(a.index, b.index) : Integer.compare(b.length, a.length));

        for (VerbMatch vm : matches) {
            String tail = t.substring(vm.index + vm.length);
            tail = LEADING_SITE_RE.matcher(tail).replaceFirst("");
            String stripped = TRAILING_DEST_RE.matcher(tail).replaceFirst("");
            pushUnique(out, stripped);
            if (!stripped.equals(tail)) {
                pushUnique(out, tail);
            }
        }

        // 3. Tail after first " for "
        int forIdx = t.toLowerCase().indexOf(" for ");
        if (forIdx >= 0) {
            pushUnique(out, TRAILING_DEST_RE.matcher(t.substring(forIdx + 5)).replaceFirst(""));
        }

        // 4. Tail after first space
        int firstSpace = t.indexOf(" ");
        if (firstSpace > 0) {
            pushUnique(out, TRAILING_DEST_RE.matcher(t.substring(firstSpace + 1)).replaceFirst(""));
        }

        // 5. Whole transcript
        pushUnique(out, t);

        if (out.size() > 8) {
            return out.subList(0, 8);
        }
        return out;
    }

    public static String normalizeSpokenUrl(String text) {
        if (text == null) return "";
        String s = cleanTranscript(text).toLowerCase();
        s = s.replaceAll("\\s+dot\\s+", ".")
                .replaceAll("\\s*\\.\\s*", ".")
                .replaceAll("\\s+slash\\s+", "/")
                .replaceAll("\\bwww\\s+", "www.");
        
        Matcher m = Pattern.compile("\\bh\\s*t\\s*t\\s*p\\s*s?\\s*:\\s*/\\s*/").matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, m.group().contains("s") ? "https://" : "http://");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static List<String> extractUrlCandidates(String transcript) {
        String t = normalizeSpokenUrl(cleanTranscript(transcript));
        if (t.isEmpty()) return Collections.emptyList();

        Pattern re = Pattern.compile("(?:https?://)?(?:[a-z0-9-]+\\.)+(?:" + TLDS + ")(?:/[^\\s]*)?", Pattern.CASE_INSENSITIVE);
        Matcher m = re.matcher(t);
        List<String> out = new ArrayList<>();
        while (m.find()) {
            String v = m.group().replaceAll("[.,!?]+$", "");
            if (!out.contains(v)) {
                out.add(v);
            }
        }
        if (out.size() > 6) {
            return out.subList(0, 6);
        }
        return out;
    }

    public static String toHttpUrl(String domainish) {
        if (domainish == null) return "https://";
        String v = domainish.trim();
        if (Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE).matcher(v).find()) {
            return v;
        }
        return "https://" + v;
    }

    private static final Map<String, Integer> NUMBER_WORDS = Map.ofEntries(
            Map.entry("one", 1), Map.entry("first", 1), Map.entry("1", 1), Map.entry("1st", 1),
            Map.entry("two", 2), Map.entry("second", 2), Map.entry("2", 2), Map.entry("2nd", 2),
            Map.entry("three", 3), Map.entry("third", 3), Map.entry("3", 3), Map.entry("3rd", 3),
            Map.entry("four", 4), Map.entry("fourth", 4), Map.entry("4", 4), Map.entry("4th", 4),
            Map.entry("five", 5), Map.entry("fifth", 5), Map.entry("5", 5), Map.entry("5th", 5)
    );

    private static final Map<String, Integer> NUMBER_HOMOPHONES = Map.of(
            "won", 1, "to", 2, "too", 2, "for", 4
    );

    private static final Set<String> PICK_STOPWORDS = Set.of(
            "the", "number", "option", "pick", "choose", "select", "click", "take", "that", "please", "link", "item", "result", "go", "with", "on", "yes", "this", "um", "uh"
    );

    public static Integer parseCandidatePick(String transcript, int max) {
        String t = cleanTranscript(transcript).toLowerCase().replaceAll("[.,!?]", "");
        if (t.isEmpty()) return null;

        String[] tokens = t.split(" ");
        List<String> meaningful = new ArrayList<>();
        for (String w : tokens) {
            if (!PICK_STOPWORDS.contains(w) && !w.isEmpty()) {
                meaningful.add(w);
            }
        }

        if (meaningful.isEmpty() || meaningful.size() > 2) return null;

        for (String w : meaningful) {
            Integer n = NUMBER_WORDS.get(w);
            if (n != null && n <= max) return n;
        }

        if (meaningful.size() == 1) {
            Integer n = NUMBER_HOMOPHONES.get(meaningful.get(0));
            if (n != null && n <= max) return n;
        }

        return null;
    }
}
