package arachnid.crawler;

import arachnid.crawler.fetch.Fetcher;

public class Main {

    public static void main(String[] args) {

        Fetcher fetcher = new Fetcher("Arachnid/1.0", "https://wwu.edu");
        Frontier frontier = new Frontier();

        for (String url : fetcher.getSitemapUrls()) {
            boolean success = frontier.offer(url, 0, "sitemap");
            System.out.println(success + ", " + url);
        }

    }
}
