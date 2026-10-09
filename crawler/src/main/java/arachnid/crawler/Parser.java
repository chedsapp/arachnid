package arachnid.crawler;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Parser {

    /**
     * Returns the absolute http(s) URL of every {@code <a href>} link in html,
     * in the order they appear. Relative links like "/about" or "../news" are
     * resolved against baseUrl.
     */
    public List<String> extractLinks(String html, String baseUrl) {
        // TODO(you): Hints:
        // - Jsoup.parse(html, baseUrl) gives you a Document that knows its
        // base URL.
        // - doc.select("a[href]") finds every link (it's a CSS selector).
        // - element.absUrl("href") resolves relative links for you, and
        // returns "" if the href can't be resolved.
        // - Skip anything that isn't http:// or https:// (mailto:, tel:,
        // javascript:, ...).
        // - Drop the "#fragment" part: "/page#top" and "/page" are the same
        // page.
        // Tests: ParserTest (run with ./gradlew :crawler:test)
        throw new UnsupportedOperationException("TODO: Parser.extractLinks");
    }

}
