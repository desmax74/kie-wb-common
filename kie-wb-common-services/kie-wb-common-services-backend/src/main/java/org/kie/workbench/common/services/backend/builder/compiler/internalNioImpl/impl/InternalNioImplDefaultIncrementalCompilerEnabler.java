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
package org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.ConfigurationContextProvider;
import org.kie.workbench.common.services.backend.builder.compiler.impl.PomPlaceHolder;
import org.kie.workbench.common.services.backend.builder.compiler.impl.ProcessedPoms;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.InternalNioImplCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.InternalNioImplIncrementalCompilerEnabler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class InternalNioImplDefaultIncrementalCompilerEnabler implements InternalNioImplIncrementalCompilerEnabler {

    private static final Logger logger = LoggerFactory.getLogger(InternalNioImplDefaultIncrementalCompilerEnabler.class);

    private final String POM_NAME = "pom.xml";

    private InternalNioImplDefaultPomEditor editor;

    public InternalNioImplDefaultIncrementalCompilerEnabler(Compilers compiler) {
        editor = new InternalNioImplDefaultPomEditor(new HashSet<PomPlaceHolder>(),
                                                     new ConfigurationContextProvider(),
                                                     compiler);
    }

    @Override
    public ProcessedPoms process(final InternalNioImplCompilationRequest req) {
        Path mainPom = Paths.get(req.getInfo().getPrjPath().toString(),
                                 POM_NAME);
        if (!Files.isReadable(mainPom)) {
            return new ProcessedPoms(Boolean.FALSE,
                                     Collections.emptyList());
        }

        PomPlaceHolder placeHolder = editor.readSingle(mainPom);
        Boolean isPresent = isPresent(placeHolder);   // check if the main pom is already scanned and edited
        if (placeHolder.isValid() && !isPresent) {
            List<String> pomsList = new ArrayList();
            InternalNioImplMavenUtils.searchPoms(Paths.get(req.getInfo().getPrjPath().toString()),
                                                 pomsList);// recursive NIO search in all subfolders
            if (pomsList.size() > 0) {
                processFoundedPoms(pomsList,
                                   req);
            }
            return new ProcessedPoms(Boolean.TRUE,
                                     pomsList);
        } else {
            return new ProcessedPoms(Boolean.FALSE,
                                     Collections.emptyList());
        }
    }

    private void processFoundedPoms(List<String> poms,
                                    InternalNioImplCompilationRequest request) {

        for (String pom : poms) {
            Path tmpPom = Paths.get(pom);
            PomPlaceHolder tmpPlaceHolder = editor.readSingle(tmpPom);
            if (!isPresent(tmpPlaceHolder)) {
                editor.write(tmpPom,
                             request);
            }
        }
        Path mainPom = Paths.get(request.getInfo().getPrjPath().toAbsolutePath().toString(),
                                 POM_NAME);
        request.getInfo().lateAdditionEnhancedMainPomFile(mainPom);
    }

    /**
     * Check if the artifact is in the hisotry
     */
    private Boolean isPresent(PomPlaceHolder placeholder) {
        return editor.getHistory().contains(placeholder);
    }

    /***
     * Return a unmodifiable history
     * @return
     */
    public Set<PomPlaceHolder> getHistory() {
        return Collections.unmodifiableSet(editor.getHistory());
    }
}
