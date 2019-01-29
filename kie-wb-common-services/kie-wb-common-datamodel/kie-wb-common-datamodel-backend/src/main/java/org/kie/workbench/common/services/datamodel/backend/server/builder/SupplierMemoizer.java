package org.kie.workbench.common.services.datamodel.backend.server.builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class SupplierMemoizer<T> {

    private final Map<Class<?>,T> memoizerSuppliers ;

    public SupplierMemoizer(){
        memoizerSuppliers = new ConcurrentHashMap<>();
    }
//DO NOT use computeIf<Absent due to the contention and problem withrecursive, use putIfAbsent
    public Supplier<T> memoizeSupplier(final Supplier<T> s) {
        ReentrantLock lock = new ReentrantLock();
        return () -> {
            lock.lock();
            try {
                return memoizerSuppliers.computeIfAbsent(SupplierMemoizer.class, i-> s.get());
            }finally {
                lock.unlock();
            }
        };
    }

    public void invalidateMemoizer(){
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            memoizerSuppliers.clear();
        }finally {
            lock.unlock();
        }
    }
}
