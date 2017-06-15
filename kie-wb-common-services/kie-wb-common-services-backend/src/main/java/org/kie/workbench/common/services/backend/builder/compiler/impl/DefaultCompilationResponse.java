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


import org.drools.core.rule.KieModuleMetaInfo;
import org.kie.api.builder.KieModule;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;

import java.util.List;
import java.util.Optional;

public class DefaultCompilationResponse implements CompilationResponse {

    private Boolean successful;
    private Optional<String> errorMessage;
    private Optional<KieModuleMetaInfo> kieModuleMetaInfo;
    private Optional<KieModule> kieModule;
    private Optional<List<String>> mavenOutput;


    public DefaultCompilationResponse(Boolean successful) {
        this.successful = successful;
        this.errorMessage = Optional.empty();
        this.kieModuleMetaInfo = Optional.empty();
        this.mavenOutput = Optional.empty();
    }

    public DefaultCompilationResponse(Boolean successful, List<String> mavenOutput) {
        this.successful = successful;
        this.errorMessage = Optional.empty();
        this.kieModuleMetaInfo = Optional.empty();
        this.mavenOutput = Optional.of(mavenOutput);
        this.mavenOutput = Optional.of(mavenOutput);
    }

    public DefaultCompilationResponse(Boolean successful, KieModuleMetaInfo kieModuleMetaInfo, KieModule kieModule) {
        this.successful = successful;
        this.errorMessage = Optional.empty();
        this.kieModuleMetaInfo = Optional.of(kieModuleMetaInfo);
        this.kieModule = Optional.of(kieModule);
        this.mavenOutput = Optional.empty();
    }

    public DefaultCompilationResponse(Boolean successful, Optional<String> errorMessage) {
        this.successful = successful;
        this.errorMessage = errorMessage;
        this.kieModuleMetaInfo = Optional.empty();
        this.mavenOutput = Optional.empty();
    }

    public DefaultCompilationResponse(Boolean successful, Optional<String> errorMessage, List<String> mavenOutput) {
        this.successful = successful;
        this.errorMessage = errorMessage;
        this.kieModuleMetaInfo = Optional.empty();
        this.mavenOutput = Optional.of(mavenOutput);
    }

    public DefaultCompilationResponse(Boolean successful, KieModuleMetaInfo kieModuleMetaInfo, KieModule kieModule, List<String> mavenOutput) {
        this.successful = successful;
        this.errorMessage = Optional.empty();
        this.kieModuleMetaInfo = Optional.of(kieModuleMetaInfo);
        this.kieModule = Optional.of(kieModule);
        this.mavenOutput = Optional.of(mavenOutput);
    }

    public Boolean isSuccessful() {
        return successful;
    }

    @Override
    public Optional<String> getErrorMessage() {
        return errorMessage;
    }

    public Optional<KieModuleMetaInfo> getKieModuleMetaInfo() {
        return kieModuleMetaInfo;
    }

    public Optional<KieModule> getKieModule(){ return kieModule; }

    public Optional<List<String>> getMavenOutput(){ return mavenOutput;}
}
