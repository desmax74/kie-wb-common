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

import org.kie.workbench.common.services.backend.builder.compiler.external339.KieCliRequest;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.InternalNioImplCompilationRequest;
import org.uberfire.java.nio.file.Path;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InternalNioImplDefaultCompilationRequest implements InternalNioImplCompilationRequest {

    private KieCliRequest req;
    private InternalNioImplWorkspaceCompilationInfo info;
    private String requestUUID;
    private Map map;

    public InternalNioImplDefaultCompilationRequest(InternalNioImplWorkspaceCompilationInfo info, String[] args, Map<String, Object> map) {
        this.info = info;
        this.requestUUID = UUID.randomUUID().toString();
        this.map = map;

        StringBuilder sb = new StringBuilder().append("-Dcompilation.ID=").append(requestUUID);
        String[] internalArgs = Arrays.copyOf(args, args.length + 1);
        internalArgs[args.length] = sb.toString();
        this.req = new KieCliRequest(this.info.getPrjPath().toAbsolutePath().toString(), internalArgs, this.map, this.requestUUID);
    }

    public String getRequestUUID() {
        return requestUUID;
    }

    @Override
    public InternalNioImplWorkspaceCompilationInfo getInfo() {
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
