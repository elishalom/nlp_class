package utils;

import tree.Node;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Queue;

/**
 * a data structure that supports
 * @param <T>
 */
public class CyclicQueue<T> extends AbstractCollection<T> implements Queue<T>, Serializable {
    public CyclicQueue(int hOrder) {
    }

    @Override
    public Iterator<T> iterator() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean offer(T t) {
        return false;
    }

    @Override
    public T remove() {
        return null;
    }

    @Override
    public T poll() {
        return null;
    }

    @Override
    public T element() {
        return null;
    }

    @Override
    public T peek() {
        return null;
    }
}
