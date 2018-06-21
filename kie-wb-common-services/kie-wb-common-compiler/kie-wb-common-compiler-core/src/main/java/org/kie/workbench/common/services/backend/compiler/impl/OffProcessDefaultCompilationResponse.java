/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.services.backend.compiler.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.drools.core.rule.KieModuleMetaInfo;
import org.kie.api.builder.KieModule;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;

/**
 * DTO for the OFFProcess Serialization to avoid problem on the Path type
 * i.e. java.io.InvalidClassException: org.uberfire.java.nio.base.GeneralPathImpl; no valid constructor
 * */
public class OffProcessDefaultCompilationResponse implements Serializable {

    private Boolean successful;
    private List<String> mavenOutput;
    private String workingDir;
    private List EMPTY_LIST = Collections.EMPTY_LIST;
    private List<String> projectDependencies = EMPTY_LIST;
    private List<String> targetContent = EMPTY_LIST;
    private KieModuleMetaInfo kieModuleMetaInfo;
    private KieModule kieModule;
    private Map<String, byte[]> projectClassLoaderStore;
    private Set<String> eventsTypeClasses;

    public OffProcessDefaultCompilationResponse(KieCompilationResponse res) {
        this.successful = res.isSuccessful();
        this.mavenOutput = res.getMavenOutput();
        if (res.getWorkingDir().isPresent()) {
            this.workingDir = res.getWorkingDir().get().toAbsolutePath().toString();
        } else {
            this.workingDir = "";
        }
        this.projectDependencies = res.getDependencies();
        this.targetContent = res.getTargetContent();
        if (res.getKieModuleMetaInfo().isPresent()) {
            this.kieModuleMetaInfo = res.getKieModuleMetaInfo().get();
        }
        if (res.getKieModule().isPresent()) {
            this.kieModule = res.getKieModule().get();
        }
        this.projectClassLoaderStore = res.getProjectClassLoaderStore();
        this.eventsTypeClasses = res.getEventTypeClasses();
    }

    public List<String> getProjectDependencies() {
        return projectDependencies;
    }

    public KieModuleMetaInfo getKieModuleMetaInfo() {
        return kieModuleMetaInfo;
    }

    public KieModule getKieModule() {
        return kieModule;
    }

    public Map<String, byte[]> getProjectClassLoaderStore() {
        return projectClassLoaderStore;
    }

    public Set<String> getEventsTypeClasses() {
        return eventsTypeClasses;
    }

    public Boolean isSuccessful() {
        return successful;
    }

    public List<String> getMavenOutput() {
        return mavenOutput;
    }

    public Optional<String> getWorkingDir() {
        return Optional.ofNullable(workingDir);
    }

    public List<String> getTargetContent() {
        return targetContent;
    }

    public List<String> getDependencies() {
        return projectDependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OffProcessDefaultCompilationResponse)) {
            return false;
        }
        OffProcessDefaultCompilationResponse that = (OffProcessDefaultCompilationResponse) o;
        return Objects.equals(successful, that.successful) &&
                Objects.equals(mavenOutput, that.mavenOutput) &&
                Objects.equals(workingDir, that.workingDir) &&
                Objects.equals(EMPTY_LIST, that.EMPTY_LIST) &&
                Objects.equals(projectDependencies, that.projectDependencies) &&
                Objects.equals(targetContent, that.targetContent) &&
                Objects.equals(kieModuleMetaInfo, that.kieModuleMetaInfo) &&
                Objects.equals(kieModule, that.kieModule) &&
                Objects.equals(projectClassLoaderStore, that.projectClassLoaderStore) &&
                Objects.equals(eventsTypeClasses, that.eventsTypeClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(successful, mavenOutput, workingDir, EMPTY_LIST, projectDependencies, targetContent, kieModuleMetaInfo, kieModule, projectClassLoaderStore, eventsTypeClasses);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OffProcessDefaultCompilationResponse{");
        sb.append("successful=").append(successful);
        sb.append(", mavenOutput=").append(mavenOutput);
        sb.append(", workingDir='").append(workingDir).append('\'');
        sb.append(", EMPTY_LIST=").append(EMPTY_LIST);
        sb.append(", projectDependencies=").append(projectDependencies);
        sb.append(", targetContent=").append(targetContent);
        sb.append(", kieModuleMetaInfo=").append(kieModuleMetaInfo);
        sb.append(", kieModule=").append(kieModule);
        sb.append(", projectClassLoaderStore=").append(projectClassLoaderStore);
        sb.append(", eventsTypeClasses=").append(eventsTypeClasses);
        sb.append('}');
        return sb.toString();
    }
}
