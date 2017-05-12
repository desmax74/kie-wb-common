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

package org.kie.workbench.common.services.backend.builder.compiler.decorators;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.MavenCompiler;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JGITCompilerDecorator extends CompilerDecorator {

    private static final Logger logger = LoggerFactory.getLogger(JGITCompilerDecorator.class);
    private final String COMPILED_EXTENSION = ".class";
    private final String REMOTE = "origin";
    private final String REMOTE_BRANCH = "master";
    private MavenCompiler compiler;

    public JGITCompilerDecorator(MavenCompiler compiler) {
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
    public CompilationResponse compileSync(CompilationRequest req) {
        if (applyBefore(req)) {
            CompilationResponse res = compiler.compileSync(req);
            applyAfter(req, res);
            return res;
        } else {
            return new DefaultCompilationResponse(Boolean.FALSE);
        }
    }

    private Boolean applyBefore(CompilationRequest req) {
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

    private void applyAfter(CompilationRequest req, CompilationResponse res) {
        //@TODO ADD JGIT push to sync the .class files
        Path prj = req.getInfo().getPrjPath().toAbsolutePath();

        Git localRepo = req.getInfo().getGitRepo().get();
        try {
            Set<String> filesChanged = localRepo.status().call().getChanged();
            Map<String, File> trackedChanged = getTrackedChanged(filesChanged, prj);

            Set<String> filesUntracked = localRepo.status().call().getUntracked();
            Map<String, File> dotClassToAdd = getUntrackedChangedByExtension(filesUntracked, prj, COMPILED_EXTENSION);

            trackedChanged.putAll(dotClassToAdd);
            if (trackedChanged.size() > 0) {
                addAndCommit(localRepo, trackedChanged);
                //@TODO push the result

            }
        } catch (GitAPIException gex) {
            logger.error(gex.getMessage());
        }
    }

    private void addAndCommit(Git localRepo, Map<String, File> trackedChanged) throws GitAPIException {
        AddCommand add = localRepo.add();
        for (String item : trackedChanged.keySet()) {
            add.addFilepattern(item);
        }
        add.call();
        localRepo.commit().setMessage("Add untracked and changed files").call();
    }


    private Map<String, File> getTrackedChanged(Set<String> changed, Path folderPath) {
        Map<String, File> map = new HashMap<>();
        for (String item : changed) {
            map.put(item, new File(folderPath.getParent().toString(), item));
        }
        return map;
    }

    private Map<String, File> getUntrackedChangedByExtension(Set<String> untracked, Path folderPath, String extension) {
        Map<String, File> map = new HashMap<>();
        //@TODO refactor with lambda
        for (String item : untracked) {
            if (item.endsWith(extension)) {
                map.put(item, new File(folderPath.getParent().toString(), item));
            }
        }
        return map;
    }
}
