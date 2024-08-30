package info.kgeorgiy.ja.matveev.arrayset;

import java.util.*;

public class ReversedList<T> implements List<T> {
    List<T> list;

    public ReversedList(List<T> list) {
        this.list = list;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("contains");
    }

    @Override
    public Iterator<T> iterator() {
        return new ReversedIterator<>(list);
    }

    private List<T> toList() {
        ArrayList<T> r = new ArrayList<>();
        for (int i = size() - 1; i >= 0; --i)
            r.add(list.get(i));
        return r;
    }

    @Override
    public Object[] toArray() {
        return toList().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return toList().toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException("add");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("containsAll");
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("addAll");
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException("addAll");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("removeAll");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("retailAll");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    @Override
    public T get(int index) {
        return list.get(size() - index - 1);
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException("set");
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException("add");
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("indexOf");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("lastIndexOf");
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException("listIterator()");
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException("listIterator(index)");
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return new ReversedList<>(list.subList(size() - toIndex - 1, size() - fromIndex - 1));
    }
}
