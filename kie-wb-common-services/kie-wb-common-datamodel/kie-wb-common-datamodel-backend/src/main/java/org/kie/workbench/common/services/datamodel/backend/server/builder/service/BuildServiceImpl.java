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

package org.kie.workbench.common.services.datamodel.backend.server.builder.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.builder.service.BuildService;
import org.guvnor.common.services.project.model.Module;
import org.guvnor.common.services.project.service.DeploymentMode;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.workbench.common.services.datamodel.backend.server.builder.ModuleBuildInfo;

@Service
@ApplicationScoped
public class BuildServiceImpl
        implements BuildService {

    private ModuleBuildInfo moduleBuildInfo;

    public BuildServiceImpl() {
        //Empty constructor for Weld
    }

    @Inject
    public BuildServiceImpl(final ModuleBuildInfo moduleBuildInfo) {
        this.moduleBuildInfo = moduleBuildInfo;

    }

    @Override
    public BuildResults build(final Module module) {
        return moduleBuildInfo.getOrCreateEntry(module).build();
    }


    @Override
    public BuildResults buildAndDeploy(final Module module) {
        return buildAndDeploy(module,
                              DeploymentMode.VALIDATED);
    }

    @Override
    public BuildResults buildAndDeploy(final Module module,
                                       final DeploymentMode mode) {

        return buildAndDeploy(module,
                              false,
                              mode);
    }

    @Override
    public BuildResults buildAndDeploy(final Module module,
                                       final boolean suppressHandlers) {
        return buildAndDeploy(module,
                              suppressHandlers,
                              DeploymentMode.VALIDATED);
    }

    @Override
    public BuildResults buildAndDeploy(final Module module,
                                       final boolean suppressHandlers,
                                       final DeploymentMode mode) {
        return moduleBuildInfo.getOrCreateEntry(module).buildAndInstall();
    }

    @Override
    public boolean isBuilt(final Module module) {
        return moduleBuildInfo.getOrCreateEntry(module).isBuilt();
    }

}