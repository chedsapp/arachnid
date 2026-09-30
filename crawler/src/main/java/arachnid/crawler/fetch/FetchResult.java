package arachnid.crawler.fetch;

import java.util.ArrayList;
import crawlercommons.robots.SimpleRobotRules;

public record FetchResult(ArrayList<String> sitemapUrls, SimpleRobotRules rules) {
}
