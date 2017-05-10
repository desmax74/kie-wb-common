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
import org.kie.workbench.common.services.backend.builder.compiler.impl.KieCliRequest;
import org.kie.workbench.common.services.backend.builder.compiler.impl.WorkspaceCompilationInfo;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DefaultIncrementalCompilerEnablerTest {

    private final Path prj = Paths.get("src/test/projects/dummy_multimodule");

    private final Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");

    @Test
    public void testReadPomsInaPrjTest() throws Exception {
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy_multimodule_untouched"), tmp);
        Path mainPom = Paths.get(tmp.toAbsolutePath().toString(), "pom.xml");

        byte[] encoded = Files.readAllBytes(Paths.get(tmp.toAbsolutePath().toString(), "pom.xml"));
        String pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertFalse(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));
        String[] args = {MavenArgs.COMPILE};
        KieCliRequest kcr = new KieCliRequest(tmp, args);

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(tmp, mavenRepo, new URI("git://repo"), new DefaultMavenCompiler(mavenRepo));
        CompilationRequest req = new DefaultCompilationRequest(kcr, info);
        DefaultIncrementalCompilerEnabler enabler = new DefaultIncrementalCompilerEnabler(Compilers.JAVAC);
        Assert.assertTrue(enabler.process(req));

        encoded = Files.readAllBytes(Paths.get(mainPom.toString()));
        pomAsAstring = new String(encoded, StandardCharsets.UTF_8);
        Assert.assertTrue(pomAsAstring.contains("<artifactId>takari-lifecycle-plugin</artifactId>"));

        TestUtil.rm(tmpRoot.toFile());
    }

}
