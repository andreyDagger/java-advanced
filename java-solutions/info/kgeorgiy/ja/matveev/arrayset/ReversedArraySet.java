package info.kgeorgiy.ja.matveev.arrayset;

import java.util.*;

public class ReversedArraySet<T> extends ArraySet<T> {

    public ReversedArraySet(final List<T> list, final Comparator<? super T> comparator) {
        this.list = list;
        this.comparator = comparator;
    }

    @Override
    protected int compare(final T t1, final T t2) {
        return super.compare(t2, t1);
    }

    @Override
    public NavigableSet<T> subSet(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromElement > toElement");
        }
        return super.subSetImpl(toElement, toInclusive, fromElement, fromInclusive).descendingSet();
    }

    @Override
    public Iterator<T> iterator() {
        return new ReversedIterator<>(list);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator == null ? null : new ReversedComparator<>(comparator);
    }

    @Override
    public ArraySet<T> descendingSet() {
        return ArraySetFromSortedList(list, comparator);
    }

    private List<T> toList() {
        final ArrayList<T> r = new ArrayList<>();
        iterator().forEachRemaining(r::add);
        return r;
    }

    @Override
    public Object[] toArray() {
        return toList().toArray();
    }

    @Override
    public <T1> T1[] toArray(final T1[] a) {
        return toList().toArray(a);
    }

    @Override
    public T lower(final T t) {
        return super.binSearch(t, false, false);
    }

    @Override
    public T floor(final T t) {
        return super.binSearch(t, false, true);
    }

    @Override
    public T ceiling(final T t) {
        return super.binSearch(t, true, true);
    }

    @Override
    public T higher(final T t) {
        return super.binSearch(t, true, false);
    }

    @Override
    public T first() {
        return super.getLast();
    }

    @Override
    public T last() {
        return super.getFirst();
    }
}
