package arachnid.crawler;

import arachnid.crawler.fetch.Fetcher;
import arachnid.crawler.fetch.FetchResult;

import arachnid.crawler.fetch.FailedFetchException;

public class Crawler {

    private String userAgent;
    private Frontier frontier;

    public Crawler(String userAgent) {
        this.userAgent = userAgent;
        this.frontier = new Frontier();
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
