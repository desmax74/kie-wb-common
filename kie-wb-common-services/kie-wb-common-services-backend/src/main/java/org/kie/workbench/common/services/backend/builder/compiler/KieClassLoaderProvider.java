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
 * Build a classloader using the dependencies founded in the poms inside a maven project
 */
public interface KieClassLoaderProvider {

    Optional<ClassLoader> loadClassloaderFrom(String path);

    Optional<ClassLoader> loadDependenciesClassloaderFromProject(String prjPath, String localRepo);

    Optional<ClassLoader> loadDependenciesClassloaderFromProject(List<String> deps, String localRepo);

    Optional<ClassLoader> loadClassesClassloaderFromProjectTargets(List<String> targets);
}

