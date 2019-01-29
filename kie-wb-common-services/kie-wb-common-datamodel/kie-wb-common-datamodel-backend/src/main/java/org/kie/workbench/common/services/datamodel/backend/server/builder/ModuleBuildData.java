/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.services.datamodel.backend.server.builder;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.guvnor.common.services.project.builder.model.BuildMessage;
import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.model.Package;
import org.kie.api.runtime.KieContainer;
import org.kie.soup.project.datamodel.oracle.ModuleDataModelOracle;
import org.kie.soup.project.datamodel.oracle.PackageDataModelOracle;
import org.uberfire.java.nio.file.Path;

public interface ModuleBuildData {

    List<BuildMessage> validate(final Path resourcePath,
                                final InputStream inputStream);

    BuildResults build();

    BuildResults buildAndInstall();

    boolean isBuilt();

    ClassLoader getClassLoader();

    ModuleDataModelOracle getModuleDataModelOracle();

    PackageDataModelOracle getPackageDataModelOracle(Package pkg);

    void reBuild(Path changedPath);

    void reBuild(Collection<Path> changedPaths);

    Optional<KieContainer> getKieContainer();
}
