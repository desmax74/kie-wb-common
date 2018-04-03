/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.services.backend.builder.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.builder.service.BuildService;
import org.guvnor.common.services.project.model.Module;
import org.guvnor.common.services.project.service.DeploymentMode;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.workbench.common.services.backend.builder.cache.ModuleCache;
import org.kie.workbench.common.services.backend.compiler.service.executors.CompilerLogLevel;

@Service
@ApplicationScoped
public class BuildServiceImpl
        implements BuildService {

    private ModuleCache moduleCache;

    public BuildServiceImpl() {
        //Empty constructor for Weld
    }

    @Inject
    public BuildServiceImpl(final ModuleCache moduleCache) {
        this.moduleCache = moduleCache;
    }

    @Override
    public BuildResults build(final Module module) {
        return buildInternal(module, CompilerLogLevel.STANDARD);
    }

    private BuildResults buildAndDeployInternal(final Module module) {
        return moduleCache.getOrCreateEntry(module).buildAndInstall(CompilerLogLevel.STANDARD);
    }

    private BuildResults buildInternal(final Module module, CompilerLogLevel logLevel) {
        return moduleCache.getOrCreateEntry(module).build(logLevel);
    }

    @Override
    public BuildResults buildAndDeploy(final Module module) {
        return buildAndDeployInternal(module);
    }

    @Override
    public BuildResults buildAndDeploy(final Module module,
                                       final DeploymentMode mode) {
        return buildAndDeployInternal(module);
    }

    @Override
    public BuildResults buildAndDeploy(final Module module,
                                       final boolean suppressHandlers) {
        return buildAndDeployInternal(module);
    }

    @Override
    public BuildResults buildAndDeploy(final Module module,
                                       final boolean suppressHandlers,
                                       final DeploymentMode mode) {
        return buildAndDeployInternal(module);
    }

    @Override
    public boolean isBuilt(final Module module) {
        return moduleCache.getOrCreateEntry(module).isBuilt();
    }
}