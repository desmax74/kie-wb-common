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

package org.kie.workbench.common.services.backend.builder.compiler;

public class MavenCompilerDecorator extends CompilerDecorator {

    private MavenCompiler compiler;

    public MavenCompilerDecorator(MavenCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public CompilationResponse compileSync(CompilationRequest req) {
        applyBefore();
        CompilationResponse res = compileSync(req);
        applyAfter();
        return res;
    }

    private void applyBefore() {
        //TODO ADD JGIT checkout before compilation
    }

    private void applyAfter() {
        //@TODO ADD JGIT pull --rebase after compilation
    }

}
