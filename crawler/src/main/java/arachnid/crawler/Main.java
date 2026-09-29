package arachnid.crawler;

import java.io.IOException;
import java.lang.InterruptedException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Fetcher fetcher = new Fetcher("Arachnid/1.0");

        fetcher.crawlUrl("https://wwu.edu");
    }
}
