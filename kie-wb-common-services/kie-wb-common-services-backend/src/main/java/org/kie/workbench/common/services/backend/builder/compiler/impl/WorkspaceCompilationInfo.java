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

import org.eclipse.jgit.api.Git;
import org.kie.workbench.common.services.backend.builder.compiler.MavenCompiler;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public class WorkspaceCompilationInfo {

    private Path prjPath;
    private Path enhancedMainPomFile;
    private URI remoteRepo;
    private MavenCompiler compiler;
    private Boolean deletePomBetweenCompilation = Boolean.FALSE;
    private Git gitRepo;

    public WorkspaceCompilationInfo(Path prjPath, Path enhancedPomFile, URI remoteRepo, MavenCompiler compiler) {
        this.prjPath = prjPath;
        this.enhancedMainPomFile = enhancedPomFile;
        this.remoteRepo = remoteRepo;
        this.compiler = compiler;
        this.gitRepo = gitRepo;
    }

    public WorkspaceCompilationInfo(Path prjPath, URI remoteRepo, MavenCompiler compiler, Git gitRepo) {
        this.prjPath = prjPath;
        this.remoteRepo = remoteRepo;
        this.compiler = compiler;
        this.gitRepo = gitRepo;
    }

    public WorkspaceCompilationInfo(Path prjPath, URI remoteRepo, MavenCompiler compiler, Boolean deletePomBetweenCompilation) {
        this.prjPath = prjPath;
        this.remoteRepo = remoteRepo;
        this.compiler = compiler;
        this.deletePomBetweenCompilation = deletePomBetweenCompilation;
    }

    public Boolean deleteEnhancedPomBetweenCompilation() {
        return deletePomBetweenCompilation;
    }

    public Boolean lateAdditionEnhancedMainPomFile(Path enhancedPom) {
        if (enhancedMainPomFile == null && enhancedPom != null) {
            this.enhancedMainPomFile = enhancedPom;
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public Boolean lateAdditionGitRepo(Git git) {
        if (gitRepo == null && git != null) {
            this.gitRepo = git;
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


    public Optional<Git> getGitRepo() {
        if (gitRepo != null) {
            return Optional.of(gitRepo);
        } else {
            return Optional.empty();
        }
    }

    public Path getPrjPath() {
        return prjPath;
    }

    public Optional<Path> getEnhancedMainPomFile() {
        if (enhancedMainPomFile != null) {
            return Optional.of(enhancedMainPomFile);
        } else {
            return Optional.empty();
        }
    }

    public Path getMavenRepo() {
        return compiler.getMavenRepo();
    }

    public URI getRemoteRepo() {
        return remoteRepo;
    }

    public MavenCompiler getCompiler() {
        return compiler;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WorkspaceCompilationInfo{");
        sb.append("prjPath=").append(prjPath);
        sb.append(", enhancedMainPomFile=").append(enhancedMainPomFile);
        sb.append(", mavenRepo=").append(compiler.getMavenRepo());
        sb.append(", remoteRepo=").append(remoteRepo);
        sb.append(", compiler=").append(compiler);
        sb.append('}');
        return sb.toString();
    }
}
