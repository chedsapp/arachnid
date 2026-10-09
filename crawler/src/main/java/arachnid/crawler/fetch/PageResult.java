package arachnid.crawler.fetch;

/**
 * A single fetched page. {@code url} is the final URL after any redirects.
 * {@code etag} and {@code lastModified} may be null if the server didn't send
 * them.
 */
public record PageResult(
        String url,
        int statusCode,
        String contentType,
        String body,
        String etag,
        String lastModified) {

    public boolean isHtml() {
        return contentType != null && contentType.startsWith("text/html");
    }
}
