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
package org.kie.workbench.common.services.backend.compiler.impl.decorators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.TestUtil;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;
import org.uberfire.java.nio.fs.jgit.JGitFileSystem;
import org.uberfire.mocks.FileSystemTestingUtils;

public class JGITCompilerBeforeDecoratorTest  {

    private static final Logger logger = LoggerFactory.getLogger(JGITCompilerBeforeDecoratorTest.class);
    private FileSystemTestingUtils fileSystemTestingUtils = new FileSystemTestingUtils();
    private IOService ioService;
    private Path mavenRepo;

    private Path tmpRootCloned;
    private Path tmpCloned;

    @Before
    public void setUp() throws Exception {
        fileSystemTestingUtils.setup();
        ioService = fileSystemTestingUtils.getIoService();

        mavenRepo = TestUtil.createMavenRepo();
    }

    @After
    public void tearDown() throws IOException {
        fileSystemTestingUtils.cleanup();
        TestUtil.rm(new File("src/../.security/"));
    }

    @Test
    public void compileTestTwo() throws Exception{
        final File gitClonedFolder = createJGitRepo("myrepodecorator");

        //Compile the repo

        Path prjFolder = Paths.get(gitClonedFolder + "/dummy/");

        JGITCompilerBeforeDecorator compiler = new JGITCompilerBeforeDecorator(new BaseMavenCompiler());
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(prjFolder);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE);
        CompilationResponse res = compiler.compile(req);

        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmpCloned, res.getMavenOutput(),
                                                      "KieDefaultMavenCompilerOnInMemoryFSTest.buildWithCloneTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        Path incrementalConfiguration = Paths.get(prjFolder + "/target/incremental/kie.io.takari.maven.plugins_kie-takari-lifecycle-plugin_compile_default-compile");
        assertThat(incrementalConfiguration.toFile().exists()).isTrue();

        if (tmpRootCloned != null) {
            TestUtil.rm(tmpRootCloned.toFile());
        }
    }

    @Test
    public void compileWithOverrideTest() throws Exception {
        final File gitClonedFolder = createJGitRepo("myrepo");

        //Compile the repo

        Path prjFolder = Paths.get(gitClonedFolder + "/dummy/");

        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(prjFolder);
        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.COMPILE},
                                                               Boolean.TRUE);


        Map<Path, InputStream> override = new HashMap<>();
        org.uberfire.java.nio.file.Path path = org.uberfire.java.nio.file.Paths.get(prjFolder + "/dummyA/src/main/java/dummy/Person.java");
        InputStream input = new FileInputStream(new File("target/test-classes/kjar-2-single-resources_override/src/main/java/dummy/PersonOverride.java"));
        override.put(path, input);

        JGITCompilerBeforeDecorator compiler = new JGITCompilerBeforeDecorator(new BaseMavenCompiler());
        CompilationResponse res = compiler.compile(req, override);

        if (!res.isSuccessful()) {
            TestUtil.writeMavenOutputIntoTargetFolder(tmpCloned, res.getMavenOutput(),
                                                      "JGITCompilerBeforeDecoratorTest.compileWithOverrideTest");
        }
        assertThat(res.isSuccessful()).isTrue();

        Path incrementalConfiguration = Paths.get(prjFolder + "/target/incremental/kie.io.takari.maven.plugins_kie-takari-lifecycle-plugin_compile_default-compile");
        assertThat(incrementalConfiguration.toFile().exists()).isTrue();


        if (tmpRootCloned != null) {
            TestUtil.rm(tmpRootCloned.toFile());
        }
    }

    private File createJGitRepo(String repoName) throws Exception {
        HashMap<String, Object> env = new HashMap<>();
        env.put("init", Boolean.TRUE);
        env.put("internal", Boolean.TRUE);

        final JGitFileSystem fs = (JGitFileSystem) ioService.newFileSystem(URI.create("git://" + repoName),
                env);

        ioService.startBatch(fs);

        ioService.write(fs.getPath("/dummy/pom.xml"),
                new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/pom.xml").toPath())));
        ioService.write(fs.getPath("/dummy/dummyA/src/main/java/dummy/DummyA.java"),
                new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/src/main/java/dummy/DummyA.java").toPath())));
        ioService.write(fs.getPath("/dummy/dummyB/src/main/java/dummy/DummyB.java"),
                new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/src/main/java/dummy/DummyB.java").toPath())));
        ioService.write(fs.getPath("/dummy/dummyA/pom.xml"),
                new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyA/pom.xml").toPath())));
        ioService.write(fs.getPath("/dummy/dummyB/pom.xml"),
                new String(java.nio.file.Files.readAllBytes(new File("src/test/projects/dummy_multimodule_untouched/dummyB/pom.xml").toPath())));
        ioService.endBatch();

        tmpRootCloned = Files.createTempDirectory("cloned");

        tmpCloned = Files.createDirectories(Paths.get(tmpRootCloned.toString(),
                "dummy"));

        final File gitClonedFolder = new File(tmpCloned.toFile(),
                ".clone.git");

        final Git cloned = Git.cloneRepository().setURI(fs.getGit().getRepository().getDirectory().toURI().toString()).setBare(false).setDirectory(gitClonedFolder).call();

        assertThat(cloned).isNotNull();

        return gitClonedFolder;
    }
}
