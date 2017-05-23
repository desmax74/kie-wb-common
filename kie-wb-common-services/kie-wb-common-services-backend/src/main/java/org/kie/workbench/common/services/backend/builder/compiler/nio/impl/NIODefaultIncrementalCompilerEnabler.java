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
package org.kie.workbench.common.services.backend.builder.compiler.nio.impl;

import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.ConfigurationContextProvider;

import org.kie.workbench.common.services.backend.builder.compiler.impl.PomPlaceHolder;
import org.kie.workbench.common.services.backend.builder.compiler.impl.ProcessedPoms;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOIncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.builder.compiler.uberfire.impl.UberfireMavenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NIODefaultIncrementalCompilerEnabler implements NIOIncrementalCompilerEnabler {

    private static final Logger logger = LoggerFactory.getLogger(NIODefaultIncrementalCompilerEnabler.class);

    private final String POM_NAME = "pom.xml";

    private NIODefaultPomEditor editor;

    public NIODefaultIncrementalCompilerEnabler(Compilers compiler) {
        editor = new NIODefaultPomEditor(new HashSet<PomPlaceHolder>(), new ConfigurationContextProvider(), compiler);
    }

    @Override
    public ProcessedPoms process(final NIOCompilationRequest req) {
        Path mainPom = Paths.get(req.getInfo().getPrjPath().toString(), POM_NAME);
        if (!Files.isReadable(mainPom)) {
            return new ProcessedPoms(Boolean.FALSE, Collections.emptyList());
        }

        PomPlaceHolder placeHolder = editor.readSingle(mainPom);
        Boolean isPresent = isPresent(placeHolder);   // check if the main pom is already scanned and edited
        if (placeHolder.isValid() && !isPresent) {
            List<String> pomsList = new ArrayList();
            NIOMavenUtils.searchPoms(Paths.get(req.getInfo().getPrjPath().toString()), pomsList);// recursive NIO search in all subfolders
            if (pomsList.size() > 0) {
                processFoundedPoms(pomsList, req);
            }
            return new ProcessedPoms(Boolean.TRUE, pomsList);
        } else {
            return new ProcessedPoms(Boolean.FALSE, Collections.emptyList());
        }
    }

    private void processFoundedPoms(List<String> poms, NIOCompilationRequest request) {

        for (String pom : poms) {
            Path tmpPom = Paths.get(pom);
            PomPlaceHolder tmpPlaceHolder = editor.readSingle(tmpPom);
            if (!isPresent(tmpPlaceHolder)) {
                editor.write(tmpPom, request);
            }
        }
        Path mainPom = Paths.get(request.getInfo().getPrjPath().toAbsolutePath().toString(), POM_NAME);
        request.getInfo().lateAdditionEnhancedMainPomFile(mainPom);
    }


    /*public void searchPoms(Path file, List<String> pomsList) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(file.toAbsolutePath())) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    searchPoms(p, pomsList);
                } else if (p.endsWith(POM_NAME)) {
                    pomsList.add(p.toAbsolutePath().toString());
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }*/

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
