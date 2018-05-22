package ru.ifmo.rain.vaksman.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(Integer.min(Runtime.getRuntime().availableProcessors(), downloaders));
        this.extractors = Executors.newFixedThreadPool(Integer.min(Runtime.getRuntime().availableProcessors(), extractors));
    }

    @Override
    public Result download(String url, int depth) {
        if (url == null || depth < 1) {
            throw new IllegalArgumentException("One of given arguments is incorrect");
        }
        List<String> downloaded = new CopyOnWriteArrayList<>();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> set = new ConcurrentSkipListSet<>(List.of(url));
        Phaser phaser = new Phaser(1);
        downloaders.submit(new DownloadTask(downloaded, errors, depth, url, 1, set, phaser));
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
        private final ConcurrentSkipListSet<String> set;
        private final Phaser phaser;

        private DownloadTask(List<String> downloaded, Map<String, IOException> errors, int maxDepth,
                             String url, int curDepth, ConcurrentSkipListSet<String> set, Phaser phaser) {
            this.downloaded = downloaded;
            this.errors = errors;
            this.maxDepth= maxDepth;
            this.url = url;
            this.curDepth= curDepth;
            this.set = set;
            this.phaser = phaser;
            phaser.register();
        }

        @Override
        public void run() {
            try {
                Document document = downloader.download(url);
                downloaded.add(url);
                if (curDepth != maxDepth) {
                    extractors.submit(new ExtractTask(downloaded, errors, maxDepth, document, url, curDepth, set, phaser));
                }
            } catch (IOException e) {
                errors.put(url, e);
            } finally {
                phaser.arrive();
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
        private final ConcurrentSkipListSet<String> set;
        private final Phaser phaser;

        private ExtractTask(List<String> downloaded, Map<String, IOException> errors, int maxDepth,
                            Document document, String url, int curDepth, ConcurrentSkipListSet<String> set, Phaser phaser) {
            this.downloaded = downloaded;
            this.errors = errors;
            this.maxDepth = maxDepth;
            this.document = document;
            this.url = url;
            this.curDepth = curDepth;
            this.set = set;
            this.phaser = phaser;
            phaser.register();
        }

        @Override
        public void run() {
            try {
                for (String s : document.extractLinks()) {
                    if (set.add(s)) {
                        downloaders.submit(new DownloadTask(downloaded, errors, maxDepth, s, curDepth + 1, set, phaser));
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