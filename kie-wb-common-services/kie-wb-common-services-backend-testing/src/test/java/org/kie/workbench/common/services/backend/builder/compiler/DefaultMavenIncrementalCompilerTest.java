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
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.impl.MavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.impl.WorkspaceCompilationInfo;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class DefaultMavenIncrementalCompilerTest {

    private final Path mavenRepo = Paths.get("src/test/resources/.ignore/m2_repo/");

    @Before
    public void setUp() throws Exception {
        if (!Files.exists(mavenRepo)) {
            System.out.println("Creating a m2_repo into src/test/resources/.ignore/m2_repo/");
            if (!Files.exists(Files.createDirectories(mavenRepo))) {
                throw new Exception("Folder not writable in the project");
            }
        }
    }

    @Test
    public void testIsValidMavenHome() throws Exception {
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy"), tmp);

        MavenCompiler compiler = MavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(tmp, compiler);
        CompilationRequest req = new DefaultCompilationRequest(info, new String[]{MavenArgs.VERSION, MavenArgs.DEBUG});
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        TestUtil.rm(tmpRoot.toFile());
    }

    @Test()
    public void testIncompleteArguments() {
        MavenCompiler compiler = MavenCompilerFactory.getCompiler(Paths.get(""), Decorator.NONE);
        Assert.assertFalse(compiler.isValid());
    }


    @Test
    public void testIncrementalWithPluginEnabled() throws Exception {
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy"), tmp);

        MavenCompiler compiler = MavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(tmp, compiler);
        CompilationRequest req = new DefaultCompilationRequest(info, new String[]{MavenArgs.CLEAN, MavenArgs.COMPILE, MavenArgs.DEBUG});
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        Path incrementalConfiguration = Paths.get(tmp.toAbsolutePath().toString(), "/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        Assert.assertTrue(incrementalConfiguration.toFile().exists());

        TestUtil.rm(tmpRoot.toFile());
    }

}
