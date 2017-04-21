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

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenGoals;

import java.io.File;
import java.util.List;

public class DefaultCompilationRequest extends DefaultInvocationRequest implements InvocationRequest, CompilationRequest {

    /**
     * Run maven with the specified maven goals (compile, test, ...)
     * in a specific prj folder
     *
     * @param baseDirPrj
     * @param offline
     * @param goals
     */
    public DefaultCompilationRequest(File baseDirPrj, boolean offline, List<String> goals) {
        if (goals == null) {
            throw new IllegalArgumentException("goals are null");
        }

        setOffline(offline);
        setBaseDirectory(baseDirPrj);
        if (goals.isEmpty()) {
            goals.add(MavenGoals.COMPILE);
        }
        setGoals(goals);
    }

    /**
     * Run maven with the specified maven goals (compile, test, ...) or with maven params
     * in a specific prj folder
     *
     * @param baseDirPrj
     * @param offline
     * @param goals
     */
    public DefaultCompilationRequest(File baseDirPrj, boolean offline, List<String> goals, String mavenOpts) {
        if (goals == null) {
            throw new IllegalArgumentException("goals are null");
        }
        setBaseDirectory(baseDirPrj);
        setOffline(offline);
        setMavenOpts(mavenOpts);
        if (goals.isEmpty()) {
            goals.add(MavenGoals.COMPILE);
        }
        setGoals(goals);
    }

}
