package ru.ifmo.rain.vaksman.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    private final TaskQueue tasks;
    private final List<Thread> threads;

    public ParallelMapperImpl(int cnt) {
        if (cnt <= 0) {
            throw new IllegalArgumentException("Incorrect amount of threads to create");
        }
        cnt = Integer.min(cnt, Runtime.getRuntime().availableProcessors());
        tasks = new TaskQueue();
        Runnable worker = () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Runnable task = tasks.getTask();
                    task.run();
                }
            } catch (InterruptedException ignored) {
            } finally {
                Thread.currentThread().interrupt();
            }
        };
        threads = Stream.generate(() -> worker)
                .limit(cnt)
                .map(Thread::new)
                .collect(Collectors.toList());
        threads.forEach(Thread::start);
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        if (f == null || args == null) {
            throw new IllegalArgumentException("One of given arguments is null");
        }
        List<R> res = new ArrayList<>(Collections.nCopies(args.size(), null));
        TaskManager manager = new TaskManager(args.size());
        for (int i = 0; i < args.size(); i++) {
            tasks.putTask(new Task<T, R>(f, args.get(i), res, i, manager));
        }
        manager.waitForResult();
        return res;
    }

    @Override
    public void close() {
        for (Thread th : threads) {
            th.interrupt();
        }
        for (Thread th : threads) {
            try {
                th.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
