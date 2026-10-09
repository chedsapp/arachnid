package arachnid.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FrontierTest {

    private final Frontier frontier = new Frontier();

    @Test
    void offerAcceptsNewUrl() {
        assertTrue(frontier.offer("https://example.com/a", 0, "link"));
        assertEquals(1, frontier.size());
        assertEquals("https://example.com/a", frontier.pop().url());
    }

    @Test
    void offerRejectsInvalidUrl() {
        assertFalse(frontier.offer("not a url", 0, "link"));
        assertTrue(frontier.isEmpty());
    }

    @Test
    void offerIgnoresDuplicates() {
        assertTrue(frontier.offer("https://example.com/a", 0, "link"));
        assertFalse(frontier.offer("https://example.com/a", 1, "link"));
        assertEquals(1, frontier.size());
    }

    @Test
    void offerIgnoresDuplicatesAfterNormalizing() {
        frontier.offer("https://example.com", 0, "link");
        frontier.offer("https://EXAMPLE.com/", 0, "link");
        assertEquals(1, frontier.size());
    }

    @Test
    void popAllowsRequeue() {
        frontier.offer("https://example.com/a", 0, "link");
        frontier.pop();
        // It was never marked visited, so it's fine to queue it again
        assertTrue(frontier.offer("https://example.com/a", 0, "link"));
    }

    @Test
    void markVisitedBlocksRequeue() {
        frontier.offer("https://example.com/a", 0, "link");
        frontier.pop();
        frontier.markVisited("https://example.com/a", null, null);

        assertFalse(frontier.offer("https://example.com/a", 0, "link"));
        assertTrue(frontier.isEmpty());
    }

    @Test
    void markVisitedUsesNormalizedUrl() {
        frontier.markVisited("https://EXAMPLE.com", null, null);

        assertFalse(frontier.offer("https://example.com/", 0, "link"));
    }

    @Test
    void visitCountIncrements() {
        assertNull(frontier.visitRecord("https://example.com/a"));

        frontier.markVisited("https://example.com/a", "\"v1\"", null);
        assertEquals(1, frontier.visitRecord("https://example.com/a").visitCount());

        frontier.markVisited("https://example.com/a", "\"v2\"", null);
        VisitRecord record = frontier.visitRecord("https://example.com/a");
        assertEquals(2, record.visitCount());
        assertEquals("\"v2\"", record.etag());
    }

}
