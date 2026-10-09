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

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public boolean offer(String url, int depth, String source) {

        String key = normalizer.filter(url);
        if (key == null)
            return false; // not a URL the normalizer understands

        // TODO(you): Don't queue a URL that's already waiting in the queue.
        // Right now, if 20 pages all link to /about, /about gets queued 20
        // times. Searching the queue itself is O(n) per offer, so keep a
        // separate Set<String> of queued URLs. Remember to remove a URL from
        // that set in pop(), or it can never be queued again for a recrawl.
        // (For thread safety later: ConcurrentHashMap.newKeySet())
        // Tests: FrontierTest.offerIgnoresDuplicates, popAllowsRequeue

        VisitRecord prev = visited.get(key);

        Instant eligibleAt = (prev == null)
                ? Instant.now()
                : prev.lastVisited().plus(recrawlInterval());

        if (prev != null && eligibleAt.isAfter(Instant.now())
                && !"sitemap".equals(source)) {
            return false;
        }

        return queue.offer(new CrawlTask(key, depth, eligibleAt));

    }

    // record url just visited
    public void markVisited(String url, String etag, Instant lastModified) {
        // TODO(you): Put a VisitRecord into `visited`. Things to get right:
        // - Use the *normalized* URL as the key, same as offer() does,
        // otherwise "https://x.com" and "https://x.com/" are different pages.
        // - If there's already a record, visitCount should go up by one;
        // otherwise it starts at 1.
        // Once this works, offer() automatically refuses to re-queue a page
        // until 14 days after its last visit.
        // Tests: FrontierTest.markVisitedBlocksRequeue, visitCountIncrements
        throw new UnsupportedOperationException("TODO: Frontier.markVisited");
    }

    // Returns the visit record for url, or null if it was never visited.
    public VisitRecord visitRecord(String url) {
        String key = normalizer.filter(url);
        return key == null ? null : visited.get(key);
    }

    private Duration recrawlInterval() {
        return baseDuration;
    }

}
