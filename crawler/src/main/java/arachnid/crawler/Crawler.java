package arachnid.crawler;

import arachnid.crawler.fetch.Fetcher;
import arachnid.crawler.fetch.FetchResult;
import arachnid.common.Database;
import arachnid.crawler.fetch.FailedFetchException;

import crawlercommons.robots.SimpleRobotRules;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

import java.nio.file.Path;
import java.nio.file.Files;

public class Crawler implements AutoCloseable {

    private static final int MAX_DEPTH = 3;
    private static final long MIN_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 30_000;

    private final Fetcher fetcher;
    private final Parser parser;
    private final Frontier frontier;
    private final Database database;

    private SimpleRobotRules robotRules;
    private String seedHost;

    public Crawler(String userAgent) throws IOException, SQLException {
        Path dbFile = Path.of(System.getProperty("user.dir"), ".mycrawler", "crawl.db");
        Files.createDirectories(dbFile.getParent());

        database = new Database(dbFile);
        fetcher = new Fetcher(userAgent);
        parser = new Parser();
        frontier = new Frontier();
    }

    public void fetchSite(String url) {

        FetchResult fetchResult;
        try {
            fetchResult = fetcher.fetch(url);
        } catch (FailedFetchException e) {
            System.err.println("Could not fetch site: " + e.getMessage());
            return;
        }

        robotRules = fetchResult.rules();
        seedHost = URI.create(url).getHost();

        // seed page incase there's no sitemap
        frontier.offer(url, 0, "seed");

        // add to frontier
        for (String sitemapUrl : fetchResult.sitemapUrls()) {
            frontier.offer(sitemapUrl, 0, "sitemap");
        }
        System.out.println("Queued " + frontier.size() + " URLs from " + seedHost);

    }

    public void crawlFrontier(int maxPages) {
        if (robotRules == null) {
            System.err.println("Call fetchSite() before crawlFrontier()");
            return;
        }

        int crawled = 0;
        while (crawled < maxPages && !frontier.isEmpty()) {
            CrawlTask task = frontier.pop();

            // TODO(you): Crawl `task`. Roughly:
            // 1. Skip it (`continue`) if:
            // - task.depth() > MAX_DEPTH
            // - task.eligibleAt() is in the future
            // - its host isn't seedHost (use isSameHost below)
            // - robotRules.isAllowed(task.url()) is false
            // 2. fetcher.fetchPage(task.url()). It throws
            // FailedFetchException on errors, so print e.getMessage() and
            // move on to the next task. One bad page shouldn't stop the crawl.
            // 3. frontier.markVisited(...) with the page's etag. (Turning the
            // Last-Modified string into an Instant is optional for now,
            // passing null is fine.)
            // 4. If page.isHtml(), use parser.extractLinks(page.body(),
            // page.url()) and offer each link to the frontier with
            // depth + 1 and source "link".
            // 5. Print something so you can watch it work, e.g.
            // "[3/50] depth 1 https://wwu.edu/about (+42 links)"
            // Skipped tasks shouldn't count toward maxPages or wait for the
            // politeness delay, so `continue` before reaching the lines below.

            crawled++;
            waitPolitely();
        }

        System.out.println("Done: crawled " + crawled + " pages, " + frontier.size() + " still queued");
    }

    // True if url is on the seed's host. "www.x.com" and "x.com" count as the same.
    private boolean isSameHost(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && stripWww(seedHost).equalsIgnoreCase(stripWww(host));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String stripWww(String host) {
        return host.regionMatches(true, 0, "www.", 0, 4) ? host.substring(4) : host;
    }

    // sleeps between requests like a kind fella
    private void waitPolitely() {
        long delay = Math.clamp(robotRules.getCrawlDelay(), MIN_DELAY_MS, MAX_DELAY_MS);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws SQLException {
        database.close();
    }

}
