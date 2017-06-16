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
package org.kie.workbench.common.services.backend.builder.compiler.nio;

import java.nio.file.Path;

import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.Valid;

/**
 * Created by Massimiliano Dessi @desmax74 on 18/04/17.
 */
public interface NIOMavenCompiler extends Valid {

    /**
     * Compile a project starting from the main POM in a sync way
     */
    CompilationResponse compileSync(NIOCompilationRequest req);

    /**
     * The local repo used by this compiler
     */
    Path getMavenRepo();
}
