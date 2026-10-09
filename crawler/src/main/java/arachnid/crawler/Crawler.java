package arachnid.crawler;

import arachnid.crawler.fetch.Fetcher;
import arachnid.crawler.fetch.FetchResult;
import arachnid.common.Database;
import arachnid.crawler.fetch.FailedFetchException;

import java.io.IOException;
import java.sql.SQLException;

import java.nio.file.Path;
import java.nio.file.Files;

public class Crawler {

    private String userAgent;
    private Frontier frontier;
    private Database database;

    public Crawler(String userAgent) {
        this.userAgent = userAgent;

        Path dbFile = Path.of(System.getProperty("user.dir"), ".mycrawler", "crawl.db");
        try {
            Files.createDirectories(dbFile.getParent());
        } catch (IOException e) {
            System.out.println("Haha failed");
        }
        try (Database db = new Database(dbFile)) {
            database = db;
            frontier = new Frontier();
        } catch (SQLException e) {
            System.out.println("Also failed");
        }
    }

    public void fetchSite(String url) {

        Fetcher siteFetcher = new Fetcher(this.userAgent);

        FetchResult fetchResult;
        try {
            fetchResult = siteFetcher.fetch(url);
        } catch (FailedFetchException e) {
            return;
        }

        // add to frontier
        for (String sitemapUrl : fetchResult.sitemapUrls()) {
            boolean success = frontier.offer(sitemapUrl, 0, "sitemap");
            System.out.println(success + ", " + sitemapUrl);
        }

    }

    public void crawlFrontier() {

    }

}
