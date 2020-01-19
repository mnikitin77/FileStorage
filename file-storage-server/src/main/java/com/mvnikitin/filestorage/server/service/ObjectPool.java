package com.mvnikitin.filestorage.server.service;

import java.util.ArrayList;
import java.util.List;

public abstract class ObjectPool<T extends Poolable> {
    protected List<T> activeList;
    protected List<T> freeList;

    public List<T> getActiveList() {
        return activeList;
    }

    // Creating a new element.
    protected abstract T newObject();

    // Moving the element from the active list to the free list.
    public void free(int index) {
        freeList.add(activeList.remove(index));
    }

    public ObjectPool() {
        this.activeList = new ArrayList<T>();
        this.freeList = new ArrayList<T>();
    }

    // Getting an element from the pool.
    public T getActiveElement() {
        if (freeList.size() == 0) {
            freeList.add(newObject());
        }
        T temp = freeList.remove(freeList.size() - 1);
        activeList.add(temp);
        return temp;
    }

    // Checking out the inactive and return them to the pool
    public void checkPool() {
        for (int i = activeList.size() - 1; i >= 0; i--) {
            if (!activeList.get(i).isActive()) {
                free(i);
            }
        }
    }

    public void clear() {
        activeList.clear();
        freeList.clear();
    }
}
