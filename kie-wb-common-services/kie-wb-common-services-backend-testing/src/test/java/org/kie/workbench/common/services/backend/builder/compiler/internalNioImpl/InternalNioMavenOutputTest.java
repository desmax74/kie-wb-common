package org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.TestUtil;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl.InternalNioImplDefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl.InternalNioImplMavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl.impl.InternalNioImplWorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.NIOMavenCompiler;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIODefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOMavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOWorkspaceCompilationInfo;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class InternalNioMavenOutputTest {

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
    public void testReadMavenHome() throws Exception {
        java.nio.file.Path tmpRoot = java.nio.file.Files.createTempDirectory("repo");
        java.nio.file.Path tmpNio = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(),
                                                                                               "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/dummy"),
                          tmpNio);

        Path tmp = Paths.get(tmpNio.toAbsolutePath().toString());

       InternalNioImplMavenCompiler compiler = InternalNioImplMavenCompilerFactory.getCompiler(mavenRepo,
                                                                                               Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        InternalNioImplWorkspaceCompilationInfo info = new InternalNioImplWorkspaceCompilationInfo(tmp,
                                                                                                   compiler);
        InternalNioImplCompilationRequest req = new InternalNioImplDefaultCompilationRequest(info,
                                                                                                                new String[]{MavenArgs.DEBUG, MavenArgs.COMPILE},
                                                                                                                new HashMap<>(),
                                                                                                                Optional.of("log"));
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        Assert.assertTrue(res.getMavenOutput().isPresent());
        Assert.assertTrue(res.getMavenOutput().get().size() > 0);
        InternalNioImplTestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void testOutputWithTakari() throws Exception {
        java.nio.file.Path tmpRoot = java.nio.file.Files.createTempDirectory("repo");
        java.nio.file.Path tmpNio = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(),
                                                                                               "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/dummy"),
                          tmpNio);

        Path tmp = Paths.get(tmpNio.toAbsolutePath().toString());

        InternalNioImplMavenCompiler compiler = InternalNioImplMavenCompilerFactory.getCompiler(mavenRepo,
                                                                                                Decorator.NONE);

        InternalNioImplWorkspaceCompilationInfo info = new InternalNioImplWorkspaceCompilationInfo(tmp,
                                                                                                   compiler);
        InternalNioImplCompilationRequest req = new InternalNioImplDefaultCompilationRequest(info,
                                                                     new String[]{MavenArgs.CLEAN, MavenArgs.COMPILE},
                                                                     new HashMap<>(),
                                                                     Optional.of("log"));
        CompilationResponse res = compiler.compileSync(req);

        Assert.assertTrue(res.getMavenOutput().isPresent());
        Assert.assertTrue(res.getMavenOutput().get().size() > 0);

        InternalNioImplTestUtil.rm(tmpRoot.toFile());
    }
}
