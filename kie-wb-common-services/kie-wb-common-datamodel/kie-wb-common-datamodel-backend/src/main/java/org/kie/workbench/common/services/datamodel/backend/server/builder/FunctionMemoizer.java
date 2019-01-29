package org.kie.workbench.common.services.datamodel.backend.server.builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class FunctionMemoizer<T,U> {

    private final Map<T, U> functionCache;

    public FunctionMemoizer() {
        functionCache = new ConcurrentHashMap<>();
    }

    private Function<T, U> doMemoize(final Function<T, U> function) {
        ReentrantLock lock = new ReentrantLock();
        return input -> {
            lock.lock();
            try {
                return functionCache.computeIfAbsent(input, function::apply);
            }finally {
                lock.unlock();
            }
        };
    }

    public  <T, U> Function<T, U> memoize(final Function<T, U> function) {
        return new FunctionMemoizer<T, U>().doMemoize(function);
    }

    public void invalidateMemoizer(){
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            functionCache.clear();
        }finally {
            lock.unlock();
        }
    }

}
