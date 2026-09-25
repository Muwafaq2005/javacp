package ai.typesafe.voicebrowser.service;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SpansExtractorTest {

    @Test
    public void testExtractTextCandidates() {
        List<String> c1 = SpansExtractor.extractTextCandidates("type \"hello world\" into the search box");
        assertTrue(c1.contains("hello world"));

        List<String> c2 = SpansExtractor.extractTextCandidates("search for alan turing");
        assertTrue(c2.contains("alan turing"));

        List<String> c3 = SpansExtractor.extractTextCandidates("look up the weather in berlin");
        assertTrue(c3.contains("the weather in berlin"));
    }

    @Test
    public void testNormalizeSpokenUrl() {
        assertEquals("example.com", SpansExtractor.normalizeSpokenUrl("example dot com"));
        assertEquals("https://example.com", SpansExtractor.toHttpUrl("example.com"));
    }

    @Test
    public void testParseCandidatePick() {
        assertEquals(1, SpansExtractor.parseCandidatePick("the first one", 5));
        assertEquals(2, SpansExtractor.parseCandidatePick("two", 5));
        assertEquals(3, SpansExtractor.parseCandidatePick("number 3", 5));
        assertNull(SpansExtractor.parseCandidatePick("something completely invalid", 5));
    }
}
