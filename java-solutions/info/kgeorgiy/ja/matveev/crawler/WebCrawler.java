package info.kgeorgiy.ja.matveev.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * Class, that allows to crawl through websites in parallel
 *
 * @author Andrey Matveev
 * @version 21
 * @see Crawler
 * @since 21
 */
public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService downloadersPull;
    private final ExecutorService extractorsPull;
    private final Map<String, DownloaderWrapper> perHostDownloader = new ConcurrentHashMap<>();

    /**
     * WebCrawler constructor
     *
     * @param downloader  Object that will be used to download websites and extract links from them
     * @param downloaders upper bound for number of threads that can simultaneously download website
     * @param extractors  upper bound for number of threads that can simultaneously extract links from websites
     * @param perHost     upper bound for number of threads that can simultaneously download websites from single host
     */
    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        if (downloaders <= 0) {
            throw new IllegalArgumentException("\"downloaders\" must pe positive");
        }
        if (extractors <= 0) {
            throw new IllegalArgumentException("\"extractors\" must pe positive");
        }
        if (perHost <= 0) {
            throw new IllegalArgumentException("\"perHost\" must pe positive");
        }
        this.downloader = downloader;
        this.perHost = perHost;
        this.downloadersPull = Executors.newFixedThreadPool(downloaders);
        this.extractorsPull = Executors.newFixedThreadPool(extractors);
    }

    private static int getOrDefault(final String[] array, final int index, final int defaultValue) {
        return index >= array.length ? defaultValue : Integer.parseInt(array[index]);
    }

    /**
     * Main function that allows to crawl websites in parallel.
     *
     * @param args Parameters in format "url [depth [downloads [extractors [perHost]]]]"
     * @see Crawler
     * @since 21
     */
    public static void main(final String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("args and his elements must be non-null");
            return;
        }
        if (args.length == 0 || args.length > 5) {
            printUsage();
            return;
        }
        final String url = args[0];
        final int depth;
        final int downloaders;
        final int extractors;
        final int perHost;
        try {
            depth = getOrDefault(args, 1, 1);
            downloaders = getOrDefault(args, 2, 100);
            extractors = getOrDefault(args, 3, 100);
            perHost = getOrDefault(args, 4, downloaders);
        } catch (final NumberFormatException e) {
            System.err.println(e.getMessage());
            printUsage();
            return;
        }
        final Downloader cachingDownloader;
        try {
            cachingDownloader = new CachingDownloader(1);
        } catch (final IOException e) {
            System.err.println("Couldn't create downloader. " + e.getMessage());
            return;
        }
        // :NOTE: use try-with-resources
        final Result result = new WebCrawler(cachingDownloader, downloaders, extractors, perHost).download(url, depth);
        System.out.println("Downloaded URLs:");
        for (final String curUrl : result.getDownloaded()) {
            System.out.println(curUrl);
        }
        System.out.println("Errors: \"[link]: [error]\"");
        for (final Map.Entry<String, IOException> entry : result.getErrors().entrySet()) {
            System.out.printf("%s: %s%n", entry.getKey(), entry.getValue());
        }
    }

    private static void printUsage() {
        System.err.println("Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]");
    }

    private List<String> getUrlsOnNextDepth(final List<UrlInfo> currentLayer, final Set<String> used, final Predicate<String> acceptFunc) {
        final List<String> result = new ArrayList<>();
        // :NOTE: use streams
        for (final UrlInfo curUrl : currentLayer) {
            for (final String nextUrl : curUrl.links) {
                if (used.contains(nextUrl) || !acceptFunc.test(nextUrl)) {
                    continue;
                }
                result.add(nextUrl);
                used.add(nextUrl);
            }
        }
        return result;
    }

    private Future<List<String>> downloadTask(final String url, final String host, final Map<String, IOException> resultErrors) {
        try {
            final Document document = downloader.download(url);
            return extractorsPull.submit(() -> {
                try {
                    return document.extractLinks();
                } catch (final IOException e) {
                    resultErrors.put(url, e);
                    return null;
                }
            });
        } catch (final IOException e) {
            resultErrors.put(url, e);
            return null;
        } finally {
            perHostDownloader.get(host).drop();
        }
    }

    private Result download(final String url, final int depth, final Predicate<String> acceptFunc) {
        if (depth <= 0 || !acceptFunc.test(url)) {
            return new Result(new ArrayList<>(), new ConcurrentHashMap<>());
        }

        final Set<String> resultUrls = ConcurrentHashMap.newKeySet();
        final ConcurrentHashMap<String, IOException> resultErrors = new ConcurrentHashMap<>();
        final Set<String> used = new HashSet<>();

        List<UrlInfo> currentLayer = new ArrayList<>();
        List<UrlInfo> nextLayer = new ArrayList<>();

        try {
            final Document document = downloader.download(url);
            final List<String> links = document.extractLinks();
            resultUrls.add(url);
            currentLayer.add(new UrlInfo(url, links));
            used.add(url);
        } catch (final IOException e) {
            resultErrors.put(url, e);
        }

        // :NOTE: use stream
        for (int d = 1; d < depth; ++d) {
            final List<String> nextUrls = getUrlsOnNextDepth(currentLayer, used, acceptFunc);

            final List<Future<Future<List<String>>>> futures = new ArrayList<>(Collections.nCopies(nextUrls.size(), null));
            for (int i = 0; i < nextUrls.size(); ++i) {
                final String nextUrl = nextUrls.get(i);
                final String host;
                try {
                    host = URLUtils.getHost(nextUrl);
                } catch (final MalformedURLException e) {
                    resultErrors.put(nextUrl, e);
                    continue;
                }
                perHostDownloader.compute(host, (key, value) -> {
                    if (value == null) {
                        return new DownloaderWrapper();
                    } else {
                        // :NOTE: Сомнительно с точки зрения многопоточности
                        value.downloadCalls++;
                        return value;
                    }
                });

                final FutureTask<Future<List<String>>> downloadTask = new FutureTask<>(() -> downloadTask(nextUrl, host, resultErrors));
                futures.set(i, downloadTask);
                perHostDownloader.get(host).run(downloadTask);
            }

            for (int i = 0; i < nextUrls.size(); ++i) {
                final List<String> links = waitTillEnd(waitTillEnd(futures.get(i)));
                if (links == null) {
                    continue;
                }
                resultUrls.add(nextUrls.get(i));
                final UrlInfo nextUrlInfo = new UrlInfo(nextUrls.get(i), links);
                nextLayer.add(nextUrlInfo);
            }
            currentLayer = nextLayer;
            nextLayer = new ArrayList<>();
        }

        perHostDownloader.forEach((key, value) -> {
            // :NOTE: Будут проблемы с многопоточностью
            if (value.downloadCalls-- == 1) {
                perHostDownloader.remove(key);
            }
        });
        return new Result(resultUrls.stream().toList(), resultErrors);
    }

    @Override
    public Result download(final String url, final int depth, final Set<String> excludes) {
        final List<String> listExcludes = excludes.stream().toList();
        return download(url, depth, s -> listExcludes.stream().noneMatch(s::contains));
    }

    @Override
    public Result download(final String url, final int depth) {
        return download(url, depth, Set.of());
    }

    @Override
    public void close() {
        downloadersPull.shutdown();
        extractorsPull.shutdown();
    }

    @Override
    public Result advancedDownload(final String url, final int depth, final List<String> hosts) {
        final HashSet<String> acceptedHosts = new HashSet<>();
        acceptedHosts.addAll(hosts);
        return download(url, depth, s -> {
            try {
                return acceptedHosts.contains(URLUtils.getHost(s));
            } catch (final MalformedURLException e) {
                throw new UncheckedMalformedUrlException(e);
            }
        });
    }

    private record UrlInfo(String url, List<String> links) {
    }

    private <T> T waitTillEnd(final Future<T> f) {
        if (f == null) { // For code simplification
            return null;
        }
        while (true) {
            try {
                return f.get();
            } catch (final InterruptedException ignored) {
            } catch (final ExecutionException e) {
                throw new AssertionError("My futures must swallow all exceptions and put them in map");
                // this scenario is impossible
            }
        }
    }

    private class DownloaderWrapper {
        private int downloadCalls = 1;
        private int downloading;
        private final Queue<Runnable> queue = new ArrayDeque<>();

        public synchronized void run(final Runnable task) {
            if (downloading < perHost) {
                ++downloading;
                downloadersPull.submit(task);
            } else {
                queue.add(task);
            }
        }

        public synchronized void drop() {
            if (queue.isEmpty()) {
                --downloading;
            } else {
                downloadersPull.submit(queue.poll());
            }
        }
    }
}
