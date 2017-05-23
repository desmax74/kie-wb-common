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

import org.kie.workbench.common.services.backend.builder.compiler.external.KieCliRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOCompilationRequest;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public class NIODefaultCompilationRequest implements NIOCompilationRequest {

    private KieCliRequest req;
    private NIOWorkspaceCompilationInfo info;

    public NIODefaultCompilationRequest(NIOWorkspaceCompilationInfo info, String[] args) {
        this.info = info;
        this.req = new KieCliRequest(info.getPrjPath().toAbsolutePath().toString(), args);
    }


    @Override
    public NIOWorkspaceCompilationInfo getInfo() {
        return info;
    }

    public Optional<URI> getRepoURI() {
        return info.getRemoteRepo();
    }

    @Override
    public Optional<Path> getPomFile() {
        return info.getEnhancedMainPomFile();
    }

    public KieCliRequest getReq() {
        return req;
    }

    @Override
    public KieCliRequest getKieCliRequest() {
        return req;
    }
}
