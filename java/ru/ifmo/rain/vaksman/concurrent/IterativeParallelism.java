package ru.ifmo.rain.vaksman.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IterativeParallelism implements ListIP {

    private class Calculate<T, R> implements Runnable {
        private List<? extends T> list;
        private List<R> dst;
        private int idx;
        private Function<List<? extends T>, R> calc;

        Calculate(List<? extends T> list, List<R> dst, int idx, Function<List<? extends T>, R> calc) {
            this.list = list;
            this.dst = dst;
            this.idx = idx;
            this.calc = calc;
        }

        @Override
        public void run() {
            dst.set(idx, calc.apply(list));
        }
    }

    private <T, R> R parallelCalc(int cnt, List<? extends T> values, Function<List<? extends T>, R> threadCalc, Function<List<R>, R> merge) throws InterruptedException {
        int size = values.size() / cnt;
        if (size == 0) {
            size = 1;
            cnt = values.size();
        }
        Thread[] threads = new Thread[cnt];
        List<R> tmp = new ArrayList<>();
        for (int i = 0; i < cnt; ++i) {
            tmp.add(null);
        }
        for (int i = 0; i < cnt; ++i) {
            threads[i] = new Thread(new Calculate<>(
                                            values.subList(i * size, i != cnt - 1 ? i * size + size : values.size()),
                                            tmp, i,
                                            threadCalc
                                        )
            );
            threads[i].start();
        }
        for (Thread th : threads) {
            th.join();
        }
        return merge.apply(tmp);
    }

    @Override
    public <T> T maximum(int cnt, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelCalc(cnt, values,
                            (List<? extends T> l) -> l.stream().max(Comparator.nullsFirst(comparator)).orElse(null),
                            (List<T> l) -> l.stream().max(Comparator.nullsFirst(comparator)).orElse(null)
                        );
    }

    @Override
    public <T> T minimum(int cnt, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelCalc(cnt, values,
                            (List<? extends T> l) -> l.stream().min(Comparator.nullsLast(comparator)).orElse(null),
                            (List<T> l) -> l.stream().min(Comparator.nullsLast(comparator)).orElse(null)
                        );
    }

    @Override
    public <T> boolean all(int cnt, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelCalc(cnt, values,
                            l -> l.stream().allMatch(predicate),
                            l -> l.stream().allMatch((x) -> x.equals(true))
                        );
    }

    @Override
    public <T> boolean any(int cnt, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelCalc(cnt, values,
                            l -> l.stream().anyMatch(predicate),
                            l -> l.stream().anyMatch((x) -> x.equals(true))
                        );
    }

    @Override
    public String join(int cnt, List<?> values) throws InterruptedException {
        return parallelCalc(cnt, values,
                            l -> l.stream().map(x -> x == null ? "" : x.toString()).collect(Collectors.joining()),
                            l -> l.stream().collect(Collectors.joining())
                        );
    }

    @Override
    public <T> List<T> filter(int cnt, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelCalc(cnt, values,
                                l -> l.stream().filter(predicate).collect(Collectors.toList()),
                                l -> l.stream().flatMap(Collection::stream).collect(Collectors.toList())
                        );
    }

    @Override
    public <T, U> List<U> map(int cnt, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelCalc(cnt, values,
                                l -> l.stream().map(f).collect(Collectors.toList()),
                                l -> l.stream().flatMap(Collection::stream).collect(Collectors.toList())
                        );
    }
}
