package ru.ifmo.rain.vaksman.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E>{
    private List<E> array;
    private Comparator<? super E> comparator;

    public ArraySet() {
        array = Collections.emptyList();
        comparator = null;
    }

    public ArraySet(Collection<? extends E> c) {
        this(c, null);
    }

    public ArraySet(Collection<? extends E> c, Comparator<? super E> comparator) {
//        HashSet<E> t = new HashSet<E>(c);
//        array = new ArrayList<E>(new HashSet<E>(c));
//        array.sort(comparator);
//        this.comparator = comparator;

        Set<E> t = new TreeSet<E>(comparator);
        t.addAll(c);
        array = new ArrayList<>(t);
        this.comparator = comparator;
    }

    private ArraySet(List<E> list, Comparator<? super E> comparator) {
        array = list;
        this.comparator = comparator;
        if (list instanceof DescendingList) {
            ((DescendingList)list).reverse();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(array).iterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<E>(new DescendingList<E>(array), Collections.reverseOrder(comparator));
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public E first() {
        if (!isEmpty())
            return array.get(0);
        throw new NoSuchElementException();
    }

    @Override
    public E last() {
        if (!isEmpty())
            return array.get(array.size() - 1);
        throw new NoSuchElementException();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(array, (E)o, comparator) >= 0;
    }

    private int anyIdx(E e, int inOffset, int notInOffset) {
        int idx = Collections.binarySearch(array, e, comparator);
        if (idx >= 0) {
            return idx + inOffset;
        }
        idx = -(idx + 1);
        return idx + notInOffset;
    }

    private int floorIdx(E e) {
        return anyIdx(e, 0, -1);
    }

    private int ceilIdx(E e) {
        return anyIdx(e, 0, 0);
    }

    private int lowerIdx(E e) {
        return anyIdx(e, -1, -1);
    }

    private int higherIdx(E e) {
        return anyIdx(e, 1, 0);
    }

    private E checkedGet(int idx) {
        return (idx >= 0 && idx < size() ? array.get(idx) : null);
    }

    @Override
    public E floor(E e) {
        return checkedGet(floorIdx(e));
    }

    @Override
    public E ceiling(E e) {
        return checkedGet(ceilIdx(e));
    }

    @Override
    public E lower(E e) {
        return checkedGet(lowerIdx(e));
    }

    @Override
    public E higher(E e) {
        return checkedGet(higherIdx(e));
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int l = (fromInclusive ? ceilIdx(fromElement) : higherIdx(fromElement));
        int r = (toInclusive ? floorIdx(toElement) : lowerIdx(toElement));
        if (l > r || l == -1 || r == -1)
            return new ArraySet<>();
        return new ArraySet<E>(array.subList(l, r + 1), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        if (isEmpty())
            return new ArraySet<E>();
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        if (isEmpty())
            return new ArraySet<E>();
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    private class DescendingList<E> extends AbstractList<E> {
        private List<E> array;
        private boolean reverse;

        DescendingList(List<E> array) {
            this.array = array;
            reverse = false;
        }

        @Override
        public E get(int index) {
            return (reverse ? array.get(size() - index - 1) : array.get(index));
        }

        @Override
        public int size() {
            return array.size();
        }

        void reverse() {
            reverse = !reverse;
        }
    }
}