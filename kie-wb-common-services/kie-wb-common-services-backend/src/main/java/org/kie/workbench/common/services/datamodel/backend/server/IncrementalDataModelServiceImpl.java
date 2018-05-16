/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.services.datamodel.backend.server;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.guvnor.common.services.backend.exceptions.ExceptionUtilities;
import org.guvnor.common.services.project.model.Package;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.soup.project.datamodel.imports.Import;
import org.kie.soup.project.datamodel.imports.Imports;
import org.kie.soup.project.datamodel.oracle.PackageDataModelOracle;
import org.kie.workbench.common.services.backend.builder.cache.ModuleCache;
import org.kie.workbench.common.services.datamodel.model.PackageDataModelOracleIncrementalPayload;
import org.kie.workbench.common.services.datamodel.service.IncrementalDataModelService;
import org.kie.workbench.common.services.shared.project.KieModule;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.uberfire.backend.vfs.Path;

import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;
import static org.kie.workbench.common.services.datamodel.backend.server.DataModelOracleUtilities.populateDataModel;

/**
 *
 */
@Service
@ApplicationScoped
public class IncrementalDataModelServiceImpl implements IncrementalDataModelService {

    private ModuleCache moduleCache;

    private KieModuleService moduleService;

    public IncrementalDataModelServiceImpl() {
    }

    @Inject
    public IncrementalDataModelServiceImpl(final ModuleCache moduleCache,
                                           final KieModuleService moduleService) {
        this.moduleCache = checkNotNull("moduleCache", moduleCache);
        this.moduleService = checkNotNull("moduleService", moduleService);
    }

    @Override
    public PackageDataModelOracleIncrementalPayload getUpdates(final Path resourcePath,
                                                               final Imports imports,
                                                               final String factType) {
        checkNotNull("resourcePath", resourcePath);
        checkNotNull("imports", imports);
        checkNotNull("factType", factType);

        final PackageDataModelOracleIncrementalPayload dataModel = new PackageDataModelOracleIncrementalPayload();

        try {
            //Check resource was within a Module structure
            final KieModule project = resolveModule(resourcePath);
            if (project == null) {
                return dataModel;
            }
            //Check resource was within a Package structure
            final Package pkg = resolvePackage(resourcePath);
            if (pkg == null) {
                return dataModel;
            }

            //Get the fully qualified class name of the fact type
            String fullyQualifiedClassName = factType;

            //Retrieve (or build) oracle and populate incremental content
            final PackageDataModelOracle oracle = moduleCache.getOrCreateEntry(project).getPackageDataModelOracle(pkg);

            // Check if the FactType is already known to the DataModelOracle, otherwise we need to find the FQCN
            if (oracle.getModuleModelFields().get(fullyQualifiedClassName) == null) {
                for (Import imp : imports.getImports()) {
                    if (imp.getType().endsWith(factType)) {
                        fullyQualifiedClassName = imp.getType();
                        break;
                    }
                }
            }

            //If the FactType isn't recognised try using the Package Name
            if (oracle.getModuleModelFields().get(fullyQualifiedClassName) == null) {
                fullyQualifiedClassName = pkg.getPackageName() + "." + factType;
            }

            //If the FactType still isn't recognised return an empty payload
            if (oracle.getModuleModelFields().get(fullyQualifiedClassName) == null) {
                return dataModel;
            }

            populateDataModel(oracle, dataModel, fullyQualifiedClassName);

            return dataModel;
        } catch (Exception e) {
            throw ExceptionUtilities.handleException(e);
        }
    }

    private KieModule resolveModule(final Path resourcePath) {
        return moduleService.resolveModule(resourcePath);
    }

    private Package resolvePackage(final Path resourcePath) {
        return moduleService.resolvePackage(resourcePath);
    }
}