package arachnid.crawler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

import crawlercommons.filters.basic.BasicURLNormalizer;

import java.util.NoSuchElementException;

record CrawlTask(String url, int depth, Instant eligibleAt) {
}

record VisitRecord(
        Instant lastVisited,
        int visitCount,
        String etag,
        Instant lastModified) {
}

public class Frontier {

    private PriorityBlockingQueue<CrawlTask> queue;
    private ConcurrentHashMap<String, VisitRecord> visited;

    private final Duration baseDuration = Duration.ofDays(14);
    private BasicURLNormalizer normalizer = new BasicURLNormalizer();

    public Frontier() {
        queue = new PriorityBlockingQueue<>(11, Comparator
                .comparing(CrawlTask::eligibleAt)
                .thenComparing(CrawlTask::depth));
        visited = new ConcurrentHashMap<String, VisitRecord>();
    }

    public CrawlTask pop() throws NoSuchElementException {
        return queue.remove();
    }

    public boolean offer(String url, int depth, String source) {

        String key = normalizer.filter(url);
        VisitRecord prev = visited.get(key);

        Instant eligableAt = (prev == null)
                ? Instant.now()
                : prev.lastVisited().plus(recrawlInterval());

        if (prev != null && eligableAt.isAfter(Instant.now())
                && !"sitemap".equals(source)) {
            return false;
        }

        return queue.offer(new CrawlTask(key, depth, eligableAt));

    }

    private Duration recrawlInterval() {
        return baseDuration;
    }

}
