/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.services.backend.validation.asset;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Charsets;
import org.guvnor.common.services.shared.builder.model.BuildMessage;
import org.guvnor.common.services.shared.message.Level;
import org.kie.workbench.common.services.backend.builder.cache.ModuleCache;
import org.kie.workbench.common.services.backend.compiler.service.executors.CompilerLogLevel;
import org.kie.workbench.common.services.shared.project.KieModule;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.io.IOService;

import static org.uberfire.backend.server.util.Paths.convert;

@ApplicationScoped
public class ValidatorBuildService {

    private final static String ERROR_CLASS_NOT_FOUND = "Definition of class \"{0}\" was not found. Consequentially validation cannot be performed.\nPlease check the necessary external dependencies for this module are configured correctly.";

    private IOService ioService;
    private KieModuleService moduleService;
    private ModuleCache moduleCache;

    public ValidatorBuildService() {
        //CDI proxies
    }

    @Inject
    public ValidatorBuildService(final @Named("ioStrategy") IOService ioService,
                                 final KieModuleService moduleService,
                                 final ModuleCache moduleCache) {
        this.ioService = ioService;
        this.moduleService = moduleService;
        this.moduleCache = moduleCache;
    }

    public List<BuildMessage> validate(final Path resourcePath,
                                       final String content) {
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(content.getBytes(Charsets.UTF_8));
            final List<BuildMessage> results = doValidation(resourcePath, inputStream);
            return results;
        } catch (NoClassDefFoundError e) {
            return error(MessageFormat.format(ERROR_CLASS_NOT_FOUND,
                                              e.getLocalizedMessage()));
        } catch (Throwable e) {
            return error(e.getLocalizedMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public List<BuildMessage> validate(final Path resourcePath) {
        InputStream inputStream = null;
        try {
            inputStream = ioService.newInputStream(convert(resourcePath));
            final List<BuildMessage> results = doValidation(resourcePath, inputStream);
            return results;
        } catch (NoClassDefFoundError e) {
            return error(MessageFormat.format(ERROR_CLASS_NOT_FOUND,
                                              e.getLocalizedMessage()));
        } catch (Throwable e) {
            return error(e.getLocalizedMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private List<BuildMessage> doValidation(final Path _resourcePath,
                                            final InputStream inputStream) throws NoModuleException {

        final Optional<KieModule> module = module(_resourcePath);
        if (!module.isPresent()) {
            return getExceptionMsgs("[ERROR] no project found");
        }

        return moduleCache.getOrCreateEntry(module.get()).validate(convert(_resourcePath), inputStream, CompilerLogLevel.STANDARD);
    }

    private Optional<KieModule> module(final Path resourcePath) throws NoModuleException {
        final KieModule module = moduleService.resolveModule(resourcePath);

        return Optional.ofNullable(module);
    }

    private List<BuildMessage> getExceptionMsgs(String msg) {
        List<BuildMessage> msgs = new ArrayList<>();
        BuildMessage msgInternal = new BuildMessage();
        msgInternal.setText(msg);
        msgs.add(msgInternal);
        return msgs;
    }

    private ArrayList<BuildMessage> error(final String errorMessage) {
        return new ArrayList<BuildMessage>() {{
            add(new BuildMessage(Level.ERROR, errorMessage));
        }};
    }
}