package arachnid.crawler.fetch;

import crawlercommons.filters.URLFilter;
import crawlercommons.filters.basic.BasicURLNormalizer;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import crawlercommons.sitemaps.SiteMapParser;
import crawlercommons.sitemaps.SiteMapURL;
import crawlercommons.sitemaps.AbstractSiteMap;
import crawlercommons.sitemaps.SiteMapIndex;
import crawlercommons.sitemaps.SiteMap;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.ArrayList;

import java.io.IOException;
import java.lang.InterruptedException;
import crawlercommons.sitemaps.UnknownFormatException;

public class Fetcher {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final URLFilter urlFilter;
    private final HttpClient httpClient;

    private final SimpleRobotRulesParser robotRulesParser;
    private final SiteMapParser sitemapParser;
    private final String userAgent;
    private final Collection<String> userAgents;

    public Fetcher(String userAgent) {
        this.userAgent = userAgent;
        urlFilter = new BasicURLNormalizer();
        httpClient = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        robotRulesParser = new SimpleRobotRulesParser();
        sitemapParser = new SiteMapParser();

        userAgents = new ArrayList<String>();
        userAgents.add(userAgent);
    }

    /**
     * Fetches robots.txt and every sitemap it lists for the site at rootUrl.
     */
    public FetchResult fetch(String rootUrl) throws FailedFetchException {

        String filteredUrl = urlFilter.filter(rootUrl);
        if (filteredUrl == null)
            throw new FailedFetchException("Invalid URL: " + rootUrl);

        // robots.txt always lives at the root of the host, whatever path we
        // were given
        URI robotsUri = URI.create(filteredUrl).resolve("/robots.txt");

        HttpResponse<byte[]> robots;
        try {
            robots = makeRequest(robotsUri.toString(), BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new FailedFetchException("Could not fetch " + robotsUri + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FailedFetchException("Interrupted fetching " + robotsUri);
        }

        SimpleRobotRules rules;
        if (robots.statusCode() == 200) {
            Collection<String> sanitizedRobotNames = SimpleRobotRulesParser.sanitizeRobotNames(userAgents);
            rules = robotRulesParser.parseContent(
                    robots.uri().toString(),
                    robots.body(),
                    "text/plain",
                    sanitizedRobotNames);
        } else {
            // e.g. 404 means "no rules, crawl anything", 5xx means "back off".
            // crawler-commons knows the conventions for each status code.
            rules = robotRulesParser.failedFetch(robots.statusCode());
        }

        ArrayList<String> sitemapUrls = new ArrayList<String>();

        if (rules.getSitemaps().size() == 0) {
            rules.addSitemap(robots.uri().resolve("/sitemap.xml").toString()); // Sometimes isn't there
        }

        for (String mapUrl : rules.getSitemaps()) {
            String filteredMapUrl = urlFilter.filter(mapUrl);
            if (filteredMapUrl == null)
                continue;

            HttpResponse<byte[]> sitemap;

            try {
                sitemap = makeRequest(filteredMapUrl, BodyHandlers.ofByteArray());
            } catch (IOException e) {
                System.err.println("Skipping sitemap " + filteredMapUrl + ": " + e.getMessage());
                continue;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (sitemap.statusCode() != 200)
                continue;

            AbstractSiteMap parsedMap;
            try {
                parsedMap = sitemapParser.parseSiteMap(sitemap.body(), sitemap.uri().toURL());
            } catch (UnknownFormatException | IOException e) {
                System.err.println("Could not parse sitemap " + filteredMapUrl + ": " + e.getMessage());
                continue;
            }

            findAllSitemaps(parsedMap, rules, sitemapUrls);
        }

        return new FetchResult(sitemapUrls, rules);
    }

    /**
     * Fetches a single page. Throws if the request fails or the server doesn't
     * answer with 200 OK.
     */
    public PageResult fetchPage(String url) throws FailedFetchException {
        HttpResponse<String> response;
        try {
            response = makeRequest(url, BodyHandlers.ofString());
        } catch (IllegalArgumentException e) {
            throw new FailedFetchException("Invalid URL: " + url);
        } catch (IOException e) {
            throw new FailedFetchException("Could not fetch " + url + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FailedFetchException("Interrupted fetching " + url);
        }

        if (response.statusCode() != 200)
            throw new FailedFetchException("HTTP " + response.statusCode() + " for " + url);

        return new PageResult(
                response.uri().toString(),
                response.statusCode(),
                response.headers().firstValue("Content-Type").orElse(null),
                response.body(),
                response.headers().firstValue("ETag").orElse(null),
                response.headers().firstValue("Last-Modified").orElse(null));
    }

    private void findAllSitemaps(AbstractSiteMap parsedMap, SimpleRobotRules rules, ArrayList<String> sitemapUrls) {
        if (parsedMap.isIndex()) {
            for (AbstractSiteMap child : ((SiteMapIndex) parsedMap).getSitemaps()) {
                findAllSitemaps(child, rules, sitemapUrls);
            }
        } else {
            for (SiteMapURL u : ((SiteMap) parsedMap).getSiteMapUrls()) {
                if (rules.isAllowed(u.getUrl().toString())) {
                    sitemapUrls.add(u.getUrl().toString());
                }
            }
        }

    }

    private <T> HttpResponse<T> makeRequest(String url, BodyHandler<T> bodyHandler)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", userAgent)
                .timeout(REQUEST_TIMEOUT)
                .build();

        return httpClient.send(request, bodyHandler);
    }

}
