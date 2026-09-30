package arachnid.crawler.fetch;

public class FailedFetchException extends Exception {
    public FailedFetchException() {
    }

    public FailedFetchException(String message) {
        super(message);
    }
}
