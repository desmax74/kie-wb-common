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
import org.kie.scanner.KieModuleMetaData;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;

import java.util.Optional;

public class DefaultCompilationResponse implements CompilationResponse {

    private Boolean successful;
    private Optional<String> errorMessage;
    private Optional<KieModule> kieModule;

    public DefaultCompilationResponse(DefaultCompilationResponse res, KieModule kieModule) {
        this.successful = res.isSuccessful();
        this.errorMessage = res.getErrorMessage();
        this.kieModule= Optional.of(kieModule);
    }

    public DefaultCompilationResponse(Boolean successful) {
        this.successful = successful;
        this.errorMessage = Optional.empty();
        this.kieModule = Optional.empty();
    }

    public DefaultCompilationResponse(Boolean successful, KieModule kieModule) {
        this.successful = successful;
        this.errorMessage = Optional.empty();
        this.kieModule = Optional.of(kieModule);
    }

    public DefaultCompilationResponse(Boolean successful, Optional<String> errorMessage) {
        this.successful = successful;
        this.errorMessage = errorMessage;
        this.kieModule = Optional.empty();
    }

    public DefaultCompilationResponse(Boolean successful, Optional<String> errorMessage, KieModule kieModule) {
        this.successful = successful;
        this.errorMessage = errorMessage;
        this.kieModule = Optional.of(kieModule);
    }

    public Boolean isSuccessful() {
        return successful;
    }

    @Override
    public Optional<String> getErrorMessage() {
        return errorMessage;
    }

    public Optional<KieModule> getKieModule() {
        return kieModule;
    }
}
