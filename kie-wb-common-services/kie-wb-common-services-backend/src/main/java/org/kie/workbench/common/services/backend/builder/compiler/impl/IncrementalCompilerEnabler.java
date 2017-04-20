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

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class IncrementalCompilerEnabler {

    private PomChangedHistory history;

    public IncrementalCompilerEnabler() {

    }

    private void searchMavenCompilerIntoPoms(InputStream is) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(is);
        List<String> modulesName = model.getModules();
        if (!modulesName.isEmpty()) {

        }

    }

    private List<String> getModules(InputStream is) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(is);
        return model.getModules();
    }

    private void searchMavenCompilerIntoPoms(String mainPomPath) throws Exception {
        File originalPom = new File(mainPomPath + "/pom.xml");
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileInputStream(originalPom));
    }

    public void disableDefaultMavenCompilerPlugin(String pomPath) throws Exception {
        File originalPom = new File(pomPath + "/pom.xml");
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileInputStream(originalPom));
        File backupPom = new File(pomPath + "/pom.backup");
        Files.copy(originalPom.toPath(), backupPom.toPath());

        //Editing
        Plugin mavenCompilerPlugin = new Plugin();
        mavenCompilerPlugin.setGroupId("org.apache.maven.plugins");
        mavenCompilerPlugin.setArtifactId("maven-compiler-plugin");
        model.getBuild().removePlugin(mavenCompilerPlugin);

        //Writing
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(new FileOutputStream(new File(pomPath, "/pom.xml")), model);
    }


}
