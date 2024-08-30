package info.kgeorgiy.ja.matveev.arrayset;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ReversedIterator<T> implements Iterator<T> {
    private final ListIterator<T> iterator;

    public ReversedIterator(List<T> list) {
        this.iterator = list.listIterator(list.size());
    }

    @Override
    public boolean hasNext() {
        return iterator.hasPrevious();
    }

    @Override
    public T next() {
        return iterator.previous();
    }
}
