package arachnid.crawler;

public class Main {

    public static void main(String[] args) {

        Crawler arachnid = new Crawler("Arachnid/1.0");

        arachnid.fetchSite("https://wwu.edu");
        arachnid.crawlFrontier();

    }
}
