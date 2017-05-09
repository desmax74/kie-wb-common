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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
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
                PullCommand pc = git.pull().setRemote("origin").setRebase(Boolean.TRUE);
                PullResult pullRes = pc.call();
                RebaseResult rr = pullRes.getRebaseResult();
                if(rr.getStatus().equals(RebaseResult.Status.UP_TO_DATE)) {
                    result = Boolean.TRUE;
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
            Set<String> filesUntracked = localRepo.status().call().getUntracked();
            Map<String, File> dotClassToAdd = getDotClassesToAdd(filesUntracked, prj.toString());
            Map<String, File> commitCandidates = getFilesToCommit(filesChanged, prj.toString());
            //@TODO complete this part
           /* JGitUtil.commit(req.getInfo().getGitRepo().get(),
                    "master",
                    "name",
                    "name@example.com",
                    "update build",
                    null,
                    null,
                    false,
                    commitCandidates
            );*/
        } catch (GitAPIException gex) {
            logger.error(gex.getMessage());
        }

    }


    private Map<String, File> getFilesToCommit(Set<String> changed, String folderPath) {
        Map<String, File> map = new HashMap<>();
        for (String item : changed) {
            map.put(item, new File(folderPath, item));
        }
        return map;
    }

    private Map<String, File> getDotClassesToAdd(Set<String> untracked, String folderPath) {
        Map<String, File> map = new HashMap<>();
        //@TODO refactor with lambda
        for (String item : untracked) {
            if(item.endsWith(".class")){
                map.put(item, new File(folderPath, item));
            }
        }
        return map;
    }
}
