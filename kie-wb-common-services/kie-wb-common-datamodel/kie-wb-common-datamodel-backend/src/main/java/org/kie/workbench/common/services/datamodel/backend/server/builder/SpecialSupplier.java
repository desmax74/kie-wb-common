package org.kie.workbench.common.services.datamodel.backend.server.builder;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class SpecialSupplier {
    /*<T> /*extends Function<T,T> {

    public T apply(AtomicReference<T> oldValue) {

        if (oldValue != null){

            asyncUpdate(oldValue);

            return oldValue;

        }
    }*/
//Do not use computeIf<Absent due to the contention and problem withrecursive, use putIfAbsent
    /*private void asyncUpdate(AtomicReference<T> oldValue) {

        Thread t1 = new Thread(() -> oldValue.set(this.get()));

        t1.start();

    }*/


   // abstract T get();
}
