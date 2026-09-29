package arachnid.crawler;

import crawlercommons.filters.URLFilter;
import crawlercommons.filters.basic.BasicURLNormalizer;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;

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

public class Fetcher {

    private URLFilter urlFilter;
    private HttpClient httpClient;

    private SimpleRobotRulesParser robotRulesParser;
    private Collection<String> userAgents;

    public Fetcher(String userAgent) {
        urlFilter = new BasicURLNormalizer();
        httpClient = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .build();

        robotRulesParser = new SimpleRobotRulesParser();

        userAgents = new ArrayList<String>();
        userAgents.add(userAgent);
    }

    public void crawlUrl(String url) throws IOException, InterruptedException {

        String filteredUrl = urlFilter.filter(url);
        if (filteredUrl == null)
            return; // Ignored

        // robots.txt
        HttpResponse<byte[]> robots = makeRequest(filteredUrl + "robots.txt");
        System.out.println(robots.statusCode());
        if (robots.statusCode() == 200) {

            Collection<String> sanitizedRobotNames = SimpleRobotRulesParser.sanitizeRobotNames(userAgents);

            SimpleRobotRules robotRules = robotRulesParser.parseContent(
                    robots.uri().toString(),
                    robots.body(),
                    "text/html",
                    sanitizedRobotNames);

            System.out.println(robotRules.toString());

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
