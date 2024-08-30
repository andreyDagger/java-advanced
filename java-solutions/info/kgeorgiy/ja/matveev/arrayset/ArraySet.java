package info.kgeorgiy.ja.matveev.arrayset;

import java.util.*;
import java.util.List;
import java.util.function.UnaryOperator;

// :NOTE: bad parent
public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T>, List<T> {
    // :NOTE: access modifiers
    List<T> list;
    Comparator<? super T> comparator;

    // :NOTE: explicit cast
    @Override
    public ArraySet<T> reversed() {
        return (ArraySet<T>) descendingSet();
    }

    @Override
    public Spliterator<T> spliterator() {
        return super.spliterator();
    }

    @Override
    public void addFirst(final T t) {
        throw new UnsupportedOperationException("addFirst");
    }

    @Override
    public void addLast(final T t) {
        throw new UnsupportedOperationException("addLast");
    }

    @Override
    public T getFirst() {
        return get(0);
    }

    @Override
    public T getLast() {
        return get(size() - 1);
    }

    // :NOTE: naming
    protected static <E> ArraySet<E> ArraySetFromSortedList(
            final List<E> list,
            final Comparator<? super E> comparator
    ) {
        final ArraySet<E> res = new ArraySet<>();
        res.list = list;
        res.comparator = comparator;
        return res;
    }

    public ArraySet() {
        list = Collections.unmodifiableList(new ArrayList<>());
        comparator = null;
    }

    public ArraySet(final Comparator<? super T> comparator) {
        list = Collections.unmodifiableList(new ArrayList<>());
        this.comparator = comparator;
    }

    public ArraySet(final Collection<T> list) {
        this(list, null);
    }

    // :NOTE: 2x unchecked
    @SuppressWarnings("unchecked")
    protected int compare(final T t1, final T t2) {
        if (comparator == null) {
            return ((Comparable<T>) t1).compareTo(t2);
        } else {
            return comparator.compare(t1, t2);
        }
    }

    public ArraySet(final Collection<T> list, final Comparator<? super T> comparator) {
        final TreeSet<T> ts = new TreeSet<>(comparator);
        ts.addAll(list);
        final ArrayList<T> al = new ArrayList<>(ts);
        this.list = Collections.unmodifiableList(al);
        this.comparator = comparator;
    }

    protected T binSearch(final T t, final boolean down, final boolean inclusive) {
        final int index = binSearchIndexed(t, down, inclusive);
        return index < 0 || size() <= index ? null : get(index);
    }

    private int binSearchIndexed(final T t, final boolean down, final boolean inclusive) {
        final int index = Collections.binarySearch(list, t, comparator);
        if (index >= 0) {
            if (inclusive) {
                return index;
            } else {
                return down ? index - 1 : index + 1;
            }
        }
        final int insertion_point = -(index + 1);
        return down ? insertion_point - 1 : insertion_point;
    }

    @Override
    public T lower(final T t) {
        return binSearch(t, true, false);
    }

    private int floorIndexed(final T t) {
        return binSearchIndexed(t, true, true);
    }

    @Override
    public T floor(final T t) {
        return binSearch(t, true, true);
    }

    @Override
    public T ceiling(final T t) {
        return binSearch(t, false, true);
    }

    @Override
    public T higher(final T t) {
        return binSearch(t, false, false);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("pollFirst");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("pollLast()");
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean contains(final Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public boolean add(final T t) {
        throw new UnsupportedOperationException("add");
    }

    @Override
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        throw new UnsupportedOperationException("addAll");
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends T> c) {
        throw new UnsupportedOperationException("addAll(index, c)");
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException("retainAll");
    }

    @Override
    public void replaceAll(final UnaryOperator<T> operator) {
        throw new UnsupportedOperationException("replaceAll");
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException("removeAll");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    // :NOTE: ds -> ss -> ds -> ss ...
    @Override
    public ArraySet<T> descendingSet() {
        return new ReversedArraySet<>(list, comparator);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    public NavigableSet<T> subSetImpl(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        final int i = binSearchIndexed(fromElement, false, fromInclusive);
        final int j = binSearchIndexed(toElement, true, toInclusive);
        if (j < i) {
            return new ArraySet<>(comparator);
        } else {
            return ArraySetFromSortedList(list.subList(i, j + 1), comparator);
        }
    }

    @Override
    public NavigableSet<T> subSet(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromElement > toElement");
        }
        return subSetImpl(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public NavigableSet<T> headSet(final T toElement, final boolean inclusive) {
        // :NOTE: return this on empty
        if (isEmpty() || compare(toElement, first()) < 0) {
            return new ArraySet<>(comparator);
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(final T fromElement, final boolean inclusive) {
        if (isEmpty() || compare(last(), fromElement) < 0) {
            return new ArraySet<>(comparator);
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(final T fromElement, final T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(final T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(final T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T removeFirst() {
        throw new UnsupportedOperationException("removeFirst");
    }

    @Override
    public T removeLast() {
        throw new UnsupportedOperationException("removeLast");
    }

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException("EXCEPTION: Calling first on empty ArraySet");
        }
        return get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException("EXCEPTION: Calling last on empty ArraySet");
        }
        return get(size() - 1);
    }

    @Override
    public T get(final int index) {
        return list.get(index);
    }

    @Override
    public T set(final int index, final T element) {
        throw new UnsupportedOperationException("set(index, element)");
    }

    @Override
    public void add(final int index, final T element) {
        throw new UnsupportedOperationException("add(index, element)");
    }

    @Override
    public T remove(final int index) {
        throw new UnsupportedOperationException("remove(index)");
    }

    @Override
    @SuppressWarnings("unchecked")
    public int indexOf(final Object o) {
        final int index = floorIndexed((T)o);
        if (index < size() && index >= 0 && compare((T)o, get(index)) == 0) {
            return index;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(final Object o) {
        return indexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
        return list.listIterator(index);
    }

    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        return list.subList(fromIndex, toIndex);
    }
}
