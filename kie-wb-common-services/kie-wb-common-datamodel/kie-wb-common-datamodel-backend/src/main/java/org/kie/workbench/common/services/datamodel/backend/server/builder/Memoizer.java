/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.workbench.common.services.datamodel.backend.server.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import org.kie.workbench.common.services.backend.compiler.impl.DefaultKieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.project.MapClassLoader;

class Memoizer<T> {

    public Memoizer() {}

    public static <T> Supplier<T> memoizeSupplier(final Supplier<T> s) {
        final Map<Class<?>,T> memoizerSuppliers = new ConcurrentHashMap<>();
        ReentrantLock lock = new ReentrantLock();
        return () -> {
            lock.lock();
            try {
                return memoizerSuppliers.computeIfAbsent(Memoizer.class, i -> s.get());
            }finally {
                lock.unlock();
            }
        };
    }


    public static <I, O> Function<I, O> memoizeFunction(Function<I, O> f) {
        Map<I, O> lookup = new HashMap<>();
        ReentrantLock lock = new ReentrantLock();
        return input -> {
            lock.lock();
            try {
                return lookup.computeIfAbsent(input, f);
            } finally {
                lock.unlock();
            }
        };
    }


    public static <T> Supplier<T> lazy(Supplier<T> supplier) {
        AtomicReference<Object> reference = new AtomicReference<>();
        return ()->{
            if (reference.compareAndSet(null, new Object())) {
                try {
                    T value = supplier.get();
                    reference.set((Supplier<T>)(()->value));

                } catch (RuntimeException e) {
                    reference.set(e);
                }
            }
            while (!(reference.get() instanceof Supplier)) {
                if (reference.get() instanceof RuntimeException)
                    throw (RuntimeException)reference.get();
            }
            return ((Supplier<T>)reference.get()).get();

        };
    }

    //@TODO impl
    public KieCompilationResponse getOrFutureEntry(CompletableFuture<T> cf) {
        return new DefaultKieCompilationResponse(false,"UUID");
    }

    //@TODO impl
    public KieCompilationResponse getValue() {
        return new DefaultKieCompilationResponse(false,"UUID");
    }

    //@TODO impl
    public ClassLoader getOrCreateEntry(ClassLoader classLoader){
        return new MapClassLoader(new HashMap<>(),this.getClass().getClassLoader());
    }






   /* public T getAndUpdate(final Supplier<Optional<T>> supplier) {
        if (currentValue.get() == null) {
        }
        return currentValue.get();
    }*/

        /*
        @TODO ask ALEX
        public boolean isInvalidated() {
            return newComputedValue == null;
        }

        public void invalidate() {
            newComputedValue = null;
        }*/
}
