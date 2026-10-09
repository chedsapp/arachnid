package arachnid.crawler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ParserTest {

    private final Parser parser = new Parser();

    @Test
    void findsAbsoluteLinks() {
        String html = "<a href=\"https://example.com/a\">A</a> <a href=\"https://other.org/b\">B</a>";

        assertEquals(
                List.of("https://example.com/a", "https://other.org/b"),
                parser.extractLinks(html, "https://example.com/"));
    }

    @Test
    void resolvesRelativeLinks() {
        String html = "<a href=\"/about\">About</a> <a href=\"news\">News</a> <a href=\"../up\">Up</a>";

        assertEquals(
                List.of("https://example.com/about",
                        "https://example.com/dept/news",
                        "https://example.com/up"),
                parser.extractLinks(html, "https://example.com/dept/"));
    }

    @Test
    void skipsNonHttpLinks() {
        String html = "<a href=\"mailto:hi@example.com\">mail</a>"
                + "<a href=\"javascript:void(0)\">js</a>"
                + "<a href=\"tel:5551234\">call</a>"
                + "<a href=\"ftp://example.com/file\">ftp</a>"
                + "<a href=\"/real\">real</a>";

        assertEquals(
                List.of("https://example.com/real"),
                parser.extractLinks(html, "https://example.com/"));
    }

    @Test
    void dropsFragments() {
        String html = "<a href=\"/page#section-2\">jump</a>";

        assertEquals(
                List.of("https://example.com/page"),
                parser.extractLinks(html, "https://example.com/"));
    }

    @Test
    void ignoresAnchorsWithoutHref() {
        String html = "<a name=\"top\">top</a> <p>no links here</p>";

        assertEquals(List.of(), parser.extractLinks(html, "https://example.com/"));
    }

}
