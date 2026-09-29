package arachnid.crawler;

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
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.util.Collection;
import java.util.ArrayList;

import java.io.IOException;
import java.lang.InterruptedException;
import crawlercommons.sitemaps.UnknownFormatException;

public class Fetcher {

    private SimpleRobotRules rules;

    private URLFilter urlFilter;
    private HttpClient httpClient;

    private SimpleRobotRulesParser robotRulesParser;
    private SiteMapParser sitemapParser;
    private Collection<String> userAgents;

    public Fetcher(String userAgent) {
        urlFilter = new BasicURLNormalizer();
        httpClient = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .build();

        robotRulesParser = new SimpleRobotRulesParser();
        sitemapParser = new SiteMapParser();

        userAgents = new ArrayList<String>();
        userAgents.add(userAgent);
    }

    public boolean isUrlAllowed(String url) {
        return rules.isAllowed(url);
    }

    public void fetchUrl(String url) throws IOException, InterruptedException, UnknownFormatException {

        String filteredUrl = urlFilter.filter(url);
        if (filteredUrl == null)
            return; // Ignored

        // robots.txt
        HttpResponse<byte[]> robots = makeRequest(filteredUrl + "robots.txt");
        System.out.println(robots.statusCode());
        if (robots.statusCode() == 200) {

            Collection<String> sanitizedRobotNames = SimpleRobotRulesParser.sanitizeRobotNames(userAgents);

            rules = robotRulesParser.parseContent(
                    robots.uri().toString(),
                    robots.body(),
                    "text/plain",
                    sanitizedRobotNames);

            if (rules.getSitemaps().size() == 0) {
                String sitemapUrl = robots.uri().toString().replace("robots.txt", "sitemap.xml");
                rules.addSitemap(sitemapUrl); // Sometimes isn't there
            }

            ArrayList<String> sitemapUrls = new ArrayList<String>();
            for (String mapUrl : rules.getSitemaps()) {
                String filteredMapUrl = urlFilter.filter(mapUrl);
                if (filteredMapUrl == null)
                    continue;

                HttpResponse<byte[]> sitemap = makeRequest(filteredMapUrl);
                if (sitemap.statusCode() != 200)
                    continue;

                AbstractSiteMap parsedMap = sitemapParser.parseSiteMap(sitemap.body(), sitemap.uri().toURL());
                findAllSitemaps(parsedMap, sitemapUrls);
            }

            for (String u : sitemapUrls) {
                System.out.println(u);
            }
        }
    }

    private void findAllSitemaps(AbstractSiteMap parsedMap, ArrayList<String> sitemapUrls) {
        if (parsedMap.isIndex()) {
            for (AbstractSiteMap child : ((SiteMapIndex) parsedMap).getSitemaps()) {
                findAllSitemaps(child, sitemapUrls);
            }
        } else {
            for (SiteMapURL u : ((SiteMap) parsedMap).getSiteMapUrls()) {
                if (rules.isAllowed(u.getUrl().toString())) {
                    sitemapUrls.add(u.getUrl().toString());
                }
            }
        }

    }

    private HttpResponse<byte[]> makeRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());

        return response;
    }

}
