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

import org.junit.Assert;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Compilers;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultIncrementalCompilerEnabler;
import org.kie.workbench.common.services.backend.builder.compiler.impl.Finder;
import org.kie.workbench.common.services.backend.builder.compiler.impl.KieCliRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DefaultIncrementalCompilerEnablerTest {

    private final Path prj = Paths.get("src/test/projects/dummy_multimodule");

    @Test
    public void testReadPomsInaPrjTest() throws Exception {
        byte[] encoded = Files.readAllBytes(Paths.get("src/test/projects/dummy_multimodule/pom.xml"));
        String pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));
        String[] args = {MavenArgs.COMPILE};
        KieCliRequest kcr = new KieCliRequest(Paths.get("src/test/projects/dummy_multimodule/"), args);
        CompilationRequest req = new DefaultCompilationRequest(kcr);
        DefaultIncrementalCompilerEnabler enabler = new DefaultIncrementalCompilerEnabler(Compilers.JAVAC);
        Assert.assertTrue(enabler.process(req));

        Finder finder = new Finder(".pom*.xml");
        Files.walkFileTree(prj, finder);
        List<Path> filesFound = finder.getFiles();
        Assert.assertTrue(filesFound.size() == 1);

        encoded = Files.readAllBytes(Paths.get(prj.toString(), filesFound.get(0).toString()));
        pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertTrue(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        Assert.assertTrue(Files.deleteIfExists(Paths.get(prj.toString(), filesFound.get(0).toString())));
    }
}
