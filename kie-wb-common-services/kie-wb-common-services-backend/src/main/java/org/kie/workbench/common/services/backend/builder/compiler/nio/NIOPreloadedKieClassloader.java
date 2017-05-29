/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.services.backend.builder.compiler.nio;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class NIOPreloadedKieClassloader extends ClassLoader {

    private static boolean isIBM_JVM = System.getProperty("java.vendor").toLowerCase().contains("ibm");
    private Map<String, byte[]> map;

    public NIOPreloadedKieClassloader() {
        map = new HashMap<>();
    }

    public NIOPreloadedKieClassloader(ClassLoader parent) {
        super(parent);
        map = new HashMap<>();
    }

    public NIOPreloadedKieClassloader(Map<String, byte[]> files, ClassLoader parent) {
        super(parent);
        map = files;
    }

    private static NIOPreloadedKieClassloader internalCreate(ClassLoader parent) {
        return isIBM_JVM ? new IBMClassLoader(parent) : new NIOPreloadedKieClassloader(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] classBytes = null;
            classBytes = loadClassBytesFromMap(name);
            if (classBytes == null) {
                classBytes = loadClassBytesFromFS(name);
            }

            Class<?> cl = defineClass(name, classBytes, 0, classBytes.length);
            if (cl == null) throw new ClassNotFoundException(name);
            return cl;

        } catch (IOException e) {
            throw new ClassNotFoundException();
        }
    }

    private byte[] loadClassBytesFromMap(String name) throws IOException {
        String className = name.replace('.', '/');
        return map.get(className);
    }

    private byte[] loadClassBytesFromFS(String name) throws IOException {
        String className = name.replace('.', '/');
        byte[] bytes = Files.readAllBytes(Paths.get(className));
        return bytes;
    }

    public static class IBMClassLoader extends NIOPreloadedKieClassloader {
        private static final Enumeration<URL> EMPTY_RESOURCE_ENUM = new Vector<URL>().elements();
        private final boolean parentImplementsFindResources;

        private IBMClassLoader(ClassLoader parent) {
            super(parent);
            Method m = null;
            try {
                m = parent.getClass().getMethod("findResources", String.class);
            } catch (NoSuchMethodException e) {
            }
            parentImplementsFindResources = m != null && m.getDeclaringClass() == parent.getClass();
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            return parentImplementsFindResources ? EMPTY_RESOURCE_ENUM : getParent().getResources(name);
        }
    }
}
