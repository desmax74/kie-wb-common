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

import com.google.common.base.Strings;

import java.util.Collections;
import java.util.Map;

/**
 * Used to track the edited poms with Takari plugins
 */
public class PomChangedHistory {

    private Map<String, String> moduleCheckedHistory; /* Used to track the edit of the poms*/

    public PomChangedHistory() {
        this.moduleCheckedHistory = moduleCheckedHistory;
    }

    public Map<String, String> getModuleCheckedHistory() {
        return moduleCheckedHistory;
    }

    /**
     * Check if the artifact is in the hisotry
     */
    public boolean isAlreadyPresent(String artifactID) {
        return !Strings.isNullOrEmpty(moduleCheckedHistory.get(artifactID));
    }

    /**
     * If isn't present it will be put in the history and the result is true, otherwise if is present the result is false
     */
    public boolean putIfAbsent(String artifactID, String path) {
        boolean check = Strings.isNullOrEmpty(moduleCheckedHistory.get(artifactID));
        if (check) {
            moduleCheckedHistory.put(artifactID, path);
        }
        return check;
    }

    /***
     * Return a unmodifiable history
     * @return
     */
    public Map<String, String> getHistory() {
        return Collections.unmodifiableMap(moduleCheckedHistory);
    }

}
