package utils;

import tree.Node;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Queue;

/**
 * a data structure that supports
 * @param <T>
 */
public class CyclicQueue<T> extends AbstractCollection<T> implements Queue<T>,BoundedCollection<T>, Serializable {
    public CyclicQueue(int hOrder) {
    }
}
