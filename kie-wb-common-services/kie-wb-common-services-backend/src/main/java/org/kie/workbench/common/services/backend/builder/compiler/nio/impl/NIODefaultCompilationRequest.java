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

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.kie.workbench.common.services.backend.builder.compiler.external339.KieCliRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOCompilationRequest;

public class NIODefaultCompilationRequest implements NIOCompilationRequest {

    private KieCliRequest req;
    private NIOWorkspaceCompilationInfo info;
    private String requestUUID;
    private Map map;

    public NIODefaultCompilationRequest(NIOWorkspaceCompilationInfo info,
                                        String[] args,
                                        Map<String, Object> map,
                                        Optional<String> logFile) {
        this.info = info;
        this.requestUUID = UUID.randomUUID().toString();
        this.map = map;
        String[] internalArgs = getInternalArgs(args,
                                                logFile);
        this.req = new KieCliRequest(info.getPrjPath().toAbsolutePath().toString(),
                                     internalArgs,
                                     this.map,
                                     this.requestUUID,
                                     logFile);
    }

    private String[] getInternalArgs(String[] args,
                                     Optional<String> logFile) {
        String[] internalArgs;
        StringBuilder sbCompilationID = new StringBuilder().append("-Dcompilation.ID=").append(requestUUID);

        if (logFile.isPresent()) {
            StringBuilder sbLogID = new StringBuilder().append("-l ").append(logFile.get()).append(".").append(requestUUID).append(".log");
            internalArgs = Arrays.copyOf(args,
                                         args.length + 2);
            internalArgs[args.length + 1] = sbLogID.toString();
        } else {
            internalArgs = Arrays.copyOf(args,
                                         args.length + 1);
        }

        internalArgs[args.length] = sbCompilationID.toString();
        return internalArgs;
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

    public String getRequestUUID() {
        return requestUUID;
    }
}
