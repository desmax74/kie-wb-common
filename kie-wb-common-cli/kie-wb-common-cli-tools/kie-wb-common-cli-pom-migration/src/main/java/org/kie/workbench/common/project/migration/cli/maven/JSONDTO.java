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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Repository;

public class JSONDTO {

    private List<Dependency> dependencies;
    private List<Repository> repositories;
    private List<Repository> pluginRepositories;

    public JSONDTO(List<Dependency> dependencies, List<Repository> respositories, List<Repository> pluginRepositories) {
        this.dependencies = dependencies;
        this.repositories = respositories;
        this.pluginRepositories = pluginRepositories;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public List<Repository> getPluginRepositories() {
        return pluginRepositories;
    }
}
