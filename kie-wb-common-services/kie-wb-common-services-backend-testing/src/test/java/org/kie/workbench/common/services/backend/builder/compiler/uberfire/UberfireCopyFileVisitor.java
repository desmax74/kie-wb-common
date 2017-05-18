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

package org.kie.workbench.common.services.backend.builder.compiler.uberfire;

import org.uberfire.java.nio.file.*;
import org.uberfire.java.nio.file.attribute.BasicFileAttributes;

public class UberfireCopyFileVisitor extends SimpleFileVisitor<Path> {

    private Path srcPath, dstPath;

    public UberfireCopyFileVisitor(Path srcPath, Path dstPath) {
        this.srcPath = srcPath;
        this.dstPath = dstPath;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        /*Path targetPath = dstPath.resolve(srcPath.relativize(dir));
        if(targetPath.toFile().exists()) {
            Boolean res = Files.deleteIfExists(targetPath);
            if(res){
                Files.move(dir, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }else{
            Files.copy(dir, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }*/

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attr) {
        Path targetPath = dstPath.resolve(srcPath.relativize(file));
        Files.copy(file, targetPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
    }

}
