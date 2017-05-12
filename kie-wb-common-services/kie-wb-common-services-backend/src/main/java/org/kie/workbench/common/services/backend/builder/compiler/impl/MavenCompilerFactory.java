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

package org.kie.workbench.common.services.backend.builder.compiler.impl;

import org.kie.workbench.common.services.backend.builder.compiler.MavenCompiler;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.decorators.JGITCompilerBeforeDecorator;
import org.kie.workbench.common.services.backend.builder.compiler.decorators.JGitCompilerAfterDecorator;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MavenCompilerFactory {

    private static Map<Path, MavenCompiler> compilers = new ConcurrentHashMap<>();

    private MavenCompilerFactory() {
    }


    /**
     * Provides a Maven compiler decorated with a Decorator Behaviour
     */
    public static MavenCompiler getCompiler(Path mavenRepo, Decorator decorator) {
        MavenCompiler compiler = compilers.get(mavenRepo);
        if (compiler == null) {
            compiler = createAndAddNewCompiler(mavenRepo, decorator);
        }
        return compiler;
    }


    private static MavenCompiler createAndAddNewCompiler(Path mavenRepo, Decorator decorator) {
        switch (decorator) {
            case NONE:
                compilers.put(mavenRepo, new DefaultMavenCompiler(mavenRepo));
                break;

            case JGIT_BEFORE:
                compilers.put(mavenRepo, new JGITCompilerBeforeDecorator(new DefaultMavenCompiler(mavenRepo)));
                break;

            case JGIT_AFTER:
                compilers.put(mavenRepo, new JGitCompilerAfterDecorator(new DefaultMavenCompiler(mavenRepo)));
                break;

            case JGIT_BEFORE_AND_AFTER:
                compilers.put(mavenRepo, new JGITCompilerBeforeDecorator(new JGitCompilerAfterDecorator(new DefaultMavenCompiler(mavenRepo))));
                break;

            default:
                compilers.put(mavenRepo, new DefaultMavenCompiler(mavenRepo));
        }
        return compilers.get(mavenRepo);
    }

    /**
     * Delete the compilers creating a new data structure
     */
    public static void deleteCompilers() {
        compilers = new ConcurrentHashMap<>();
    }


    /**
     * Clear the internal data structure
     */
    public static void clearCompilers() {
        compilers.clear();
    }


    public static void removeCompiler(Path mavenRepo) {
        compilers.remove(mavenRepo);
    }

    public static void replaceCompiler(Path mavenRepo, MavenCompiler compiler) {
        compilers.replace(mavenRepo, compiler);
    }

}
