package org.kie.workbench.common.services.backend.builder.compiler.nio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.builder.compiler.TestUtil;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.Decorator;
import org.kie.workbench.common.services.backend.builder.compiler.configuration.MavenArgs;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIODefaultCompilationRequest;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOMavenCompilerFactory;
import org.kie.workbench.common.services.backend.builder.compiler.nio.impl.NIOWorkspaceCompilationInfo;

public class NioMavenOutputTest {

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
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy"), tmp);

        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(tmp, compiler);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info, new String[]{MavenArgs.VERSION}, new HashMap<>(), new ArrayList<>());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        Assert.assertTrue(res.getMavenOutput().isPresent());
        List<String> outPut = res.getMavenOutput().get();
        System.out.println("\nOutput from CompilerResponse>>>>\n::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
        for(String line : outPut){
            System.out.println(line);
        }
        System.out.println("::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::\nEnd output");
        TestUtil.rm(tmpRoot.toFile());
    }

    @Test
    public void testHelpOutput() throws Exception {
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy"), tmp);

        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(tmp, compiler);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info, new String[]{"-h"}, new HashMap<>());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());

        Assert.assertTrue(res.getMavenOutput().isPresent());
        List<String> outPut = res.getMavenOutput().get();
        System.out.println("\nOutput from CompilerResponse>>>>\n::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
        for(String line : outPut){
            System.out.println(line);
        }
        System.out.println("::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::\nEnd output");
        TestUtil.rm(tmpRoot.toFile());
    }


    @Test
    public void testIncrementalWithPluginEnabled() throws Exception {
        Path tmpRoot = Files.createTempDirectory("repo");
        Path tmp = Files.createDirectories(Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(Paths.get("src/test/projects/dummy"), tmp);

        NIOMavenCompiler compiler = NIOMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);

        NIOWorkspaceCompilationInfo info = new NIOWorkspaceCompilationInfo(tmp, compiler);
        NIOCompilationRequest req = new NIODefaultCompilationRequest(info, new String[]{MavenArgs.CLEAN, MavenArgs.COMPILE}, new HashMap<>());
        CompilationResponse res = compiler.compileSync(req);
        //Assert.assertTrue(res.isSuccessful());

        Assert.assertTrue(res.getMavenOutput().isPresent());
        List<String> outPut = res.getMavenOutput().get();
        System.out.println("\nOutput from CompilerResponse>>>>\n::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
        for(String line : outPut){
            System.out.println(line);
        }
        System.out.println("::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::\nEnd output");

        //Path incrementalConfiguration = Paths.get(tmp.toAbsolutePath().toString(), "/target/incremental/io.takari.maven.plugins_takari-lifecycle-plugin_compile_compile");
        //Assert.assertTrue(incrementalConfiguration.toFile().exists());

        TestUtil.rm(tmpRoot.toFile());
    }

}
