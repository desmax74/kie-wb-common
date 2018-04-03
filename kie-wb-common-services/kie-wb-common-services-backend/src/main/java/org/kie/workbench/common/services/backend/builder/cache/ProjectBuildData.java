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
 *
 */

package org.kie.workbench.common.services.backend.builder.cache;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.model.Package;
import org.guvnor.common.services.shared.builder.model.BuildMessage;
import org.kie.api.runtime.KieContainer;
import org.kie.soup.project.datamodel.oracle.ModuleDataModelOracle;
import org.kie.soup.project.datamodel.oracle.PackageDataModelOracle;
import org.kie.workbench.common.services.backend.compiler.service.executors.CompilerLogLevel;
import org.uberfire.java.nio.file.Path;

public interface ProjectBuildData {

    List<BuildMessage> validate(final Path resourcePath,
                                final InputStream inputStream, CompilerLogLevel logLevel);

    BuildResults build(CompilerLogLevel logLevel);

    BuildResults buildAndInstall(CompilerLogLevel logLevel);

    boolean isBuilt();

    ClassLoader getClassLoader();

    ModuleDataModelOracle getModuleDataModelOracle();

    PackageDataModelOracle getPackageDataModelOracle(Package pkg);

    void reBuild(Path changedPath);

    void reBuild(Collection<Path> changedPaths);

    Optional<KieContainer> getKieContainer();
}
