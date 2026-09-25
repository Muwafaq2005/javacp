package ai.typesafe.voicebrowser.service;

import ai.typesafe.voicebrowser.model.ElementSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SnapshotCollectorTest {

    @Test
    public void testDetectSite() {
        assertEquals("wikipedia", SnapshotCollector.detectSite("https://en.wikipedia.org/wiki/Main_Page"));
        assertEquals("hacker_news", SnapshotCollector.detectSite("https://news.ycombinator.com/newest"));
        assertEquals("google", SnapshotCollector.detectSite("https://www.google.com/search?q=x"));
        assertEquals("example_com", SnapshotCollector.detectSite("https://example.com/"));
        assertEquals("generic", SnapshotCollector.detectSite("https://foo.bar.baz/"));
        assertEquals("blank", SnapshotCollector.detectSite("about:blank"));
    }

    @Test
    public void testFindSearchBox() {
        ElementSnapshot e1 = new ElementSnapshot();
        e1.setId("e01");
        e1.setRole("textbox");
        e1.setPlaceholder("Username");

        ElementSnapshot e2 = new ElementSnapshot();
        e2.setId("e02");
        e2.setRole("textbox");
        e2.setPlaceholder("Search Wikipedia");

        assertEquals("e02", SnapshotCollector.findSearchBox(List.of(e1, e2)));
    }
}
