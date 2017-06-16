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

package org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.uberfire.java.nio.file.FileSystems;
import org.uberfire.java.nio.file.FileVisitResult;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.PathMatcher;
import org.uberfire.java.nio.file.SimpleFileVisitor;
import org.uberfire.java.nio.file.attribute.BasicFileAttributes;

import static org.uberfire.java.nio.file.FileVisitResult.CONTINUE;

public class InternalNioImplFinder extends SimpleFileVisitor<Path> {

    private final PathMatcher matcher;
    private List<Path> files;

    public InternalNioImplFinder(String pattern) {
        matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        files = new ArrayList<>();
    }

    public List<Path> getFiles() {
        return files;
    }

    void find(Path file) {
        Path name = file.getFileName();
        if (name != null && matcher.matches(name)) {
            files.add(name);
        }
    }

    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attrs) {
        find(file);
        return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir,
                                             BasicFileAttributes attrs) {
        find(dir);
        return CONTINUE;
    }

    public FileVisitResult visitFileFailed(Path file,
                                           IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }
}
