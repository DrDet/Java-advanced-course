package ru.ifmo.rain.vaksman.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    private class Calculate<T, R> implements Runnable {
        private List<? extends T> list;
        private List<R> dst;
        private int idx;
        private Function<Stream<? extends T>, R> calc;

        Calculate(List<? extends T> list, List<R> dst, int idx, Function<Stream<? extends T>, R> calc) {
            this.list = list;
            this.dst = dst;
            this.idx = idx;
            this.calc = calc;
        }

        @Override
        public void run() {
            dst.set(idx, calc.apply(list.stream()));
        }
    }

    private void checkForNull(Object x) {
        if (x == null) {
            throw new IllegalArgumentException("One of given argument is null");
        }
    }

    private <T, R> R parallelCalc(int cnt, List<? extends T> values, Function<Stream<? extends T>, R> threadCalc, Function<Stream<R>, R> merge) throws InterruptedException {
        if (cnt <= 0 || values == null) {
            throw new IllegalArgumentException("Incorrect amount of threads or given list is null");
        }
        int blockSize = values.size() / cnt;
        int extra = values.size() % cnt;
        if (blockSize == 0) {
            cnt = values.size();
            blockSize = 1;
            extra = 0;
        }
        Thread[] threads = new Thread[cnt];
        List<R> tmp = new ArrayList<>(Collections.nCopies(cnt, null));
        for (int i = 0, l = 0, curSize; i < cnt; i++) {
            curSize = blockSize;
            if (extra > 0) {
                curSize++;
                extra--;
            }
            threads[i] = new Thread(new Calculate<>(
                                            values.subList(l, l + curSize),
                                            tmp, i,
                                            threadCalc
                                        )
            );
            l += curSize;
            threads[i].start();
        }
        for (Thread th : threads) {
            th.join();              //TODO: to process interrupted exception
        }
        return merge.apply(tmp.stream());
    }

    @Override
    public <T> T maximum(int cnt, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelCalc(cnt, values,
                            s -> s.max(Comparator.nullsFirst(comparator)).orElse(null),
                            s -> s.max(Comparator.nullsFirst(comparator)).orElse(null)
                        );
    }

    @Override
    public <T> T minimum(int cnt, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelCalc(cnt, values,
                            s -> s.min(Comparator.nullsLast(comparator)).orElse(null),
                            s -> s.min(Comparator.nullsLast(comparator)).orElse(null)
                        );
    }

    @Override
    public <T> boolean all(int cnt, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        checkForNull(predicate);
        return parallelCalc(cnt, values,
                            s -> s.allMatch(predicate),
                            s -> s.allMatch((x) -> x.equals(true))
                        );
    }

    @Override
    public <T> boolean any(int cnt, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        checkForNull(predicate);
        return parallelCalc(cnt, values,
                            s -> s.anyMatch(predicate),
                            s -> s.anyMatch(x -> x)
                        );
    }

    @Override
    public String join(int cnt, List<?> values) throws InterruptedException {
        return parallelCalc(cnt, values,
                            s -> s.map(x -> x == null ? "" : x.toString()).collect(Collectors.joining()),
                            s -> s.collect(Collectors.joining())
                        );
    }

    @Override
    public <T> List<T> filter(int cnt, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        checkForNull(predicate);
        return parallelCalc(cnt, values,
                                s -> s.filter(predicate).collect(Collectors.toList()),
                                s -> s.flatMap(Collection::stream).collect(Collectors.toList())
                        );
    }

    @Override
    public <T, U> List<U> map(int cnt, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        checkForNull(f);
        return parallelCalc(cnt, values,
                                s -> s.map(f).collect(Collectors.toList()),
                                s -> s.flatMap(Collection::stream).collect(Collectors.toList())
                        );
    }
}
