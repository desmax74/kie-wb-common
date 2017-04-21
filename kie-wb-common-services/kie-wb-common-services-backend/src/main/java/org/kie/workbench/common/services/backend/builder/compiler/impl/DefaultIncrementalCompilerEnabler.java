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

import org.apache.commons.io.FileUtils;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.IncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.ConfigurationStaticStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class DefaultIncrementalCompilerEnabler implements IncrementalCompilerEnabler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultIncrementalCompilerEnabler.class);

    private final String POM_NAME = "pom.xml";

    private DefaultPomEditor editor;

    public DefaultIncrementalCompilerEnabler(Compilers compiler) {
        editor = new DefaultPomEditor(new HashSet<PomPlaceHolder>(), new ConfigurationStaticStrategy(), compiler);
    }

    @Override
    public Boolean process(final CompilationRequest req) {

        File mainPom = FileUtils.getFile(req.getBaseDirectory(), POM_NAME);
        if (!mainPom.isFile()) {
            return Boolean.FALSE;
        }

        PomPlaceHolder placeHolder = editor.readSingle(mainPom);
        Boolean isPresent = isPresent(placeHolder);   // check if the main pom is already scanned and edited
        if (placeHolder.isValid() && !isPresent) {
            List<String> pomsList = new ArrayList();
            searchPoms(req.getBaseDirectory(), pomsList);// recursive search in all subfolders
            if (pomsList.size() > 0) {
                for (String pom : pomsList) {
                    processSinglePom(pom);
                }
            }
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }

    }

    private void processSinglePom(String pom) {
        File tmpPom = FileUtils.getFile(pom);
        PomPlaceHolder tmplaceHolder = editor.readSingle(tmpPom);

        if (!isPresent(tmplaceHolder)) {
            editor.write(tmpPom);
        }

    }


    private void searchPoms(File file, List<String> pomsList) {

        if (file.isDirectory()) {
            if (file.canRead()) {

                for (File tmp : file.listFiles()) {
                    if (tmp.isDirectory()) {
                        searchPoms(tmp, pomsList);
                    } else {
                        if (POM_NAME.equals(tmp.getName().toLowerCase())) {
                            pomsList.add(tmp.getAbsoluteFile().toString());
                        }
                    }
                }

            } else {
                logger.error("Permission Denied on:" + file.getAbsoluteFile());
            }
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
