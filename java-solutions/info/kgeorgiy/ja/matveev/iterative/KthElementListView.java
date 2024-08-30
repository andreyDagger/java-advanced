package info.kgeorgiy.ja.matveev.iterative;

import java.util.AbstractList;
import java.util.List;

/**
 * Class, that gives view on every k-th element of underlying list.
 *
 * @param <E> Type, that will be stored in this list
 * @author Andrey Matveev
 * @see AbstractList
 * @version 21
 * @since 21
 */
public class KthElementListView<E> extends AbstractList<E> {

    private final List<E> originalList;
    private final int k;
    private final int size;

    /**
     * Constructor for view.
     * Creates view of {@code originalList} on every k-th element of this list
     *
     * @param originalList List, whose view we're creating
     * @param k Parameter for our view
     */
    public KthElementListView(List<E> originalList, int k) {
        this.originalList = originalList;
        this.k = k;
        this.size = (originalList.size() + k - 1) / k;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        return originalList.get(index * k);
    }

    @Override
    public int size() {
        return size;
    }
}
