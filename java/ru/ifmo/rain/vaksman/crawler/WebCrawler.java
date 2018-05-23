package ru.ifmo.rain.vaksman.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

import static info.kgeorgiy.java.advanced.crawler.URLUtils.getHost;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        if (downloader == null || downloaders < 1 || extractors < 1 || perHost < 1) {
            throw new IllegalArgumentException("One of given arguments is incorrect");
        }
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(Integer.min(Runtime.getRuntime().availableProcessors(), downloaders));
        this.extractors = Executors.newFixedThreadPool(Integer.min(Runtime.getRuntime().availableProcessors(), extractors));
        this.perHost = perHost;
    }

    private class HostTraffic {
        private int cnt;
        private final Queue<Runnable> extra;

        private HostTraffic() {
            cnt = 1;
            extra = new LinkedList<>();
        }

        private synchronized void addTask(Runnable task) {
            if (cnt < perHost) {
                downloaders.submit(task);
                cnt++;
            } else {
                extra.add(task);
            }
        }

        private synchronized Runnable getTask() {
            if (extra.isEmpty()) {
                cnt--;
            }
            return extra.poll();
        }
    }

    @Override
    public Result download(String url, int depth) {
        if (url == null || depth < 1) {
            throw new IllegalArgumentException("One of given arguments is incorrect");
        }
        String host;
        try {
            host = getHost(url);
        } catch (MalformedURLException e) {
            IllegalArgumentException exception = new IllegalArgumentException("Couldn't get host of given url");
            exception.addSuppressed(e);
            throw exception;
        }
        List<String> downloaded = new CopyOnWriteArrayList<>();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> pages = new ConcurrentSkipListSet<>(List.of(url));
        ConcurrentHashMap<String, HostTraffic> hosts = new ConcurrentHashMap<>(Map.of(host, new HostTraffic()));
        Phaser phaser = new Phaser(1);
        downloaders.submit(new DownloadTask(downloaded, errors, depth, url, 1, pages, hosts, phaser));
        phaser.arriveAndAwaitAdvance();
        return new Result(downloaded, errors);
    }

    @Override
    public void close() {
        extractors.shutdownNow();
        downloaders.shutdownNow();
    }

    private class DownloadTask implements Runnable {
        private final List<String> downloaded;
        private final Map<String, IOException> errors;
        private final int maxDepth;
        private final String url;
        private final int curDepth;
        private final ConcurrentSkipListSet<String> pages;
        private final ConcurrentHashMap<String, HostTraffic> hosts;
        private final Phaser phaser;

        private DownloadTask(List<String> downloaded, Map<String, IOException> errors, int maxDepth,
                             String url, int curDepth,
                             ConcurrentSkipListSet<String> pages, ConcurrentHashMap<String, HostTraffic> hosts,
                             Phaser phaser) {
            this.downloaded = downloaded;
            this.errors = errors;
            this.maxDepth= maxDepth;
            this.url = url;
            this.curDepth= curDepth;
            this.pages = pages;
            this.hosts = hosts;
            this.phaser = phaser;
            phaser.register();
        }

        @Override
        public void run() {
            try {
                Document document = downloader.download(url);
                downloaded.add(url);
                if (curDepth != maxDepth) {
                    extractors.submit(new ExtractTask(downloaded, errors, maxDepth, document, url, curDepth, pages, hosts, phaser));
                }
            } catch (IOException e) {
                errors.put(url, e);
            } finally {
                phaser.arrive();
                String host = null; //this url is got from Document::extractLinks -> assume that's correct
                try {
                    host = getHost(url);
                } catch (MalformedURLException impossible) {
                    System.err.println("IMPOSSIBLE");
                }
                assert host != null;
                Runnable downloadTask = hosts.get(host).getTask();
                if (downloadTask != null) {
                    downloaders.submit(downloadTask);
                }
            }
        }
    }

    private class ExtractTask implements Runnable {
        private final List<String> downloaded;
        private final Map<String, IOException> errors;
        private final int maxDepth;
        private final Document document;
        private final String url;
        private final int curDepth;
        private final ConcurrentSkipListSet<String> pages;
        private final ConcurrentHashMap<String, HostTraffic> hosts;
        private final Phaser phaser;

        private ExtractTask(List<String> downloaded, Map<String, IOException> errors, int maxDepth,
                            Document document, String url, int curDepth,
                            ConcurrentSkipListSet<String> pages, ConcurrentHashMap<String, HostTraffic> hosts,
                            Phaser phaser) {
            this.downloaded = downloaded;
            this.errors = errors;
            this.maxDepth = maxDepth;
            this.document = document;
            this.url = url;
            this.curDepth = curDepth;
            this.pages = pages;
            this.hosts = hosts;
            this.phaser = phaser;
            phaser.register();
        }

        @Override
        public void run() {
            try {
                for (String s : document.extractLinks()) {
                    if (pages.add(s)) {
                        String host = getHost(s);
                        Runnable downloadTask = new DownloadTask(downloaded, errors, maxDepth, s, curDepth + 1, pages, hosts, phaser);
                        if (!hosts.containsKey(host)) {
                            hosts.put(host, new HostTraffic());
                            downloaders.submit(downloadTask);
                        } else {
                            hosts.get(host).addTask(downloadTask);
                        }
                    }
                }
            } catch (IOException e) {
                errors.put(url, e);
            } finally {
                phaser.arrive();
            }
        }
    }
}