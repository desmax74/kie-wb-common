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

package org.kie.workbench.common.services.backend.builder.compiler;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface KieClassLoaderProvider {

    /**
     * Build a classloader using the dependencies declared (transitive excluded) in the poms inside the modules
     * @param prjPath
     * @param localRepo
     * @return
     */
    Optional<ClassLoader> loadDependenciesClassloaderFromProject(String prjPath,
                                                                 String localRepo);

    /**
     * Build a classloader using the dependencies declared (transitive excluded) in the poms inside the modules
     * @param deps
     * @param localRepo
     * @return
     */
    Optional<ClassLoader> loadDependenciesClassloaderFromProject(List<String> deps,
                                                                 String localRepo);

    /**
     * Build a classloader using the target folders of the modules in the project
     * @param targets
     * @param loadIntoClassloader
     * @return
     */
    Optional<ClassLoader> getClassloaderFromProjectTargets(List<String> targets,
                                                           Boolean loadIntoClassloader);

    Optional<ClassLoader> loadDependenciesClassloaderFromProject(String prjPath,
                                                                 String localRepo,
                                                                 ClassLoader parentClassloader);

    Optional<ClassLoader> loadDependenciesClassloaderFromProject(List<String> deps,
                                                                 String localRepo,
                                                                 ClassLoader parentClassloader);

    Optional<ClassLoader> getClassloaderFromProjectTargets(List<String> targets,
                                                           Boolean loadIntoClassloader,
                                                           ClassLoader parentClassloader);

    /**
     * Build a classloader with all the dependencies (included transitive) present in all the prj modules
     * @param prjPath
     * @param localRepo
     * @return
     */
    Optional<ClassLoader> getClassloaderFromAllDependencies(String prjPath,
                                                            String localRepo);
}

