package info.kgeorgiy.ja.matveev.arrayset;

import java.util.Comparator;

public class ReversedComparator<T> implements Comparator<T> {
    // :NOTE: Comparator.reverseOrder
    // :NOTE: access modifiers
    Comparator<? super T> comparator;
    public ReversedComparator(Comparator<? super T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public int compare(T o1, T o2) {
        return comparator.compare(o2, o1);
    }
}
