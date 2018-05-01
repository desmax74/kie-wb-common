/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.project.migration.cli.maven;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.model.Dependency;

public class DependenciesCollection {

    private HashSet<DependencyKey> keys;

    public DependenciesCollection() {
        keys = new HashSet<>();
    }

    public void addDependencies(List<Dependency> deps) {
        for (Dependency dep : deps) {
            DependencyKey key = new DependencyKey(dep);
            keys.add(key);
        }
    }

    public void addDependenciesKeys(List<DependencyKey> deps) {
        keys.addAll(deps);
    }

    public List<Dependency> getAsDependencyList() {
        List<Dependency> deps = new ArrayList<>(keys.size());
        for (DependencyKey key : keys) {
            deps.add(key.getDependency());
        }
        return deps;
    }
}
