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

package org.kie.workbench.common.services.backend.builder.compiler.nio.decorators;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.decorators.CompilerDecorator;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOMavenCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassloaderAfterDecorator extends CompilerDecorator {

    private static final Logger logger = LoggerFactory.getLogger(JGITCompilerBeforeDecorator.class);

    private NIOMavenCompiler compiler;

    public ClassloaderAfterDecorator(NIOMavenCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public Boolean isValid() {
        return compiler.isValid();
    }

    @Override
    public CompilationResponse compileSync(NIOCompilationRequest req) {

        CompilationResponse res = compiler.compileSync(req);
        if (res.isSuccessful()) {
            try {
                applyAfter(req,
                           res);
            } catch (MalformedURLException muex) {
                logger.error(muex.getMessage());
            }
        }
        return res;
    }

    @Override
    public Path getMavenRepo() {
        return compiler.getMavenRepo();
    }

    private void applyAfter(NIOCompilationRequest req,
                            CompilationResponse res) throws MalformedURLException {
        File classDir = new File(req.getInfo().getPrjPath().toFile(),
                                 "target/classes");

        System.out.println(classDir);

        List<URL> classloadingURLs = new ArrayList<>();
        classloadingURLs.add(classDir.toURI().toURL());

        File libDir = new File(req.getInfo().getPrjPath().toFile(),
                               "target/lib");
        for (File jar : libDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir,
                                  String name) {
                return name.endsWith(".jar");
            }
        })) {
            classloadingURLs.add(jar.toURI().toURL());
        }
        ClassLoader cl = new URLClassLoader(classloadingURLs.toArray(new URL[]{}),
                                            null);
        //@TODO add the changes

    }
}
