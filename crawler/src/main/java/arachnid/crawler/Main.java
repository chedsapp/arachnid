package arachnid.crawler;

import java.io.IOException;
import java.lang.InterruptedException;
import crawlercommons.sitemaps.UnknownFormatException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, UnknownFormatException {
        Fetcher fetcher = new Fetcher("Arachnid/1.0");

        fetcher.fetchUrl("https://wwu.edu");
    }
}
