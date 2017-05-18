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

package org.kie.workbench.common.services.backend.builder.compiler.uberfire.decorators;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.uberfire.UberfireCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.uberfire.UberfireMavenCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Path;


public class UberfireJGITCompilerBeforeDecorator extends UberfireCompilerDecorator {

    private static final Logger logger = LoggerFactory.getLogger(UberfireJGITCompilerBeforeDecorator.class);
    private final String COMPILED_EXTENSION = ".class";
    private final String REMOTE = "origin";
    private final String REMOTE_BRANCH = "master";
    private UberfireMavenCompiler compiler;

    public UberfireJGITCompilerBeforeDecorator(UberfireMavenCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public Boolean isValid() {
        return compiler.isValid();
    }

    @Override
    public Path getMavenRepo() {
        return compiler.getMavenRepo();
    }

    @Override
    public CompilationResponse compileSync(UberfireCompilationRequest req) {
        if (applyBefore(req)) {
            CompilationResponse res = compiler.compileSync(req);
            return res;
        } else {
            return new DefaultCompilationResponse(Boolean.FALSE);
        }
    }

    private Boolean applyBefore(UberfireCompilationRequest req) {
        Boolean result = Boolean.FALSE;
        if (req.getInfo().getGitRepo().isPresent()) {
            try {
                Git git = req.getInfo().getGitRepo().get();
                PullCommand pc = git.pull().setRemote(REMOTE).setRebase(Boolean.TRUE);
                PullResult pullRes = pc.call();
                RebaseResult rr = pullRes.getRebaseResult();

                if (rr.getStatus().equals(RebaseResult.Status.UP_TO_DATE) || rr.getStatus().equals(RebaseResult.Status.FAST_FORWARD)) {
                    result = Boolean.TRUE;
                }
                if (rr.getStatus().equals(RebaseResult.Status.UNCOMMITTED_CHANGES)) {
                    PullResult pr = git.pull().call();
                    if (pr.isSuccessful()) {
                        result = Boolean.TRUE;
                    } else {
                        result = Boolean.FALSE;
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
            return result;
        }
        return result;
    }
}
