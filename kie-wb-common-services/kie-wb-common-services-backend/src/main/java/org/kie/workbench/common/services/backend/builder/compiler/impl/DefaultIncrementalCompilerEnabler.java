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
package org.kie.workbench.common.services.backend.builder.compiler.impl;

import org.kie.workbench.common.services.backend.builder.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.IncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.ConfigurationContextStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DefaultIncrementalCompilerEnabler implements IncrementalCompilerEnabler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultIncrementalCompilerEnabler.class);

    private final String POM_NAME = "pom.xml";

    private DefaultPomEditor editor;

    public DefaultIncrementalCompilerEnabler(Compilers compiler, Boolean writeOnFS) {
        editor = new DefaultPomEditor(new HashSet<PomPlaceHolder>(), new ConfigurationContextStrategy(), compiler, writeOnFS);
    }

    @Override
    public Boolean process(final CompilationRequest req) {
        Path mainPom = Paths.get(req.getBaseDirectory().toString(), POM_NAME);
        if (!Files.isReadable(mainPom)) {
            return Boolean.FALSE;
        }

        PomPlaceHolder placeHolder = editor.readSingle(mainPom);
        Boolean isPresent = isPresent(placeHolder);   // check if the main pom is already scanned and edited
        if (placeHolder.isValid() && !isPresent) {
            List<String> pomsList = new ArrayList();
            searchPoms(Paths.get(req.getBaseDirectory().toString()), pomsList);// recursive NIO search in all subfolders
            if (pomsList.size() > 0) {
                processSingleFoundedPoms(pomsList);
            }
            //@TODO set the input stream with POM changed in the CompilationRequest
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }

    }

    private void processSingleFoundedPoms(List<String> poms) {

        for (String pom : poms) {
            Path tmpPom = Paths.get(pom);
            PomPlaceHolder tmpPlaceHolder = editor.readSingle(tmpPom);
            if (!isPresent(tmpPlaceHolder)) {
                editor.write(tmpPom.toFile());
            }
        }
    }


    private void searchPoms(Path file, List<String> pomsList) {
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
