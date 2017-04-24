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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultIncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenGoals;
import org.uberfire.java.nio.fs.jgit.JGitFileSystem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class DefaultIncrementalCompilerEnablerTest {

    private final File prj  =new File("src/test/projects/dummy_multimodule");

    private final File cleanPom  =new File("src/test/projects/dummy_multimodule_untouched/pom.xml");

    @Test
    public void testReadPomsInaPrjTest() throws Exception{
        byte[] encoded = Files.readAllBytes(Paths.get("src/test/projects/dummy_multimodule/pom.xml"));
        String pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        CompilationRequest req = new DefaultCompilationRequest(prj, Boolean.TRUE, Arrays.asList(MavenGoals.COMPILE));
        DefaultIncrementalCompilerEnabler enabler = new DefaultIncrementalCompilerEnabler(Compilers.JAVAC, Boolean.TRUE);
        Assert.assertTrue(enabler.process(req));
        encoded = Files.readAllBytes(Paths.get("src/test/projects/dummy_multimodule/pom.xml"));
        pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertTrue(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));
    }



    @After
    public void restoreOriginalPom() throws IOException {
        FileUtils.copyFile(cleanPom, new File("src/test/projects/dummy_multimodule/pom.xml"));
    }
}
