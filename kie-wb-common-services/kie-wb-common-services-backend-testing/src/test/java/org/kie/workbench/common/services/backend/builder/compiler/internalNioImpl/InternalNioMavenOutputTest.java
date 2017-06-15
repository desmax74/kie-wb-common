package org.kie.workbench.common.services.backend.builder.compiler.internalNioImpl;



import java.util.HashMap;
import java.util.List;

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
    public void testIsValidMavenHome() throws Exception {
        org.uberfire.java.nio.file.Path tmpRoot = org.uberfire.java.nio.file.Files.createTempDirectory("repo");
        org.uberfire.java.nio.file.Path tmp = org.uberfire.java.nio.file.Files.createDirectories(org.uberfire.java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        //NIO creation and copy content
        java.nio.file.Path temp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/dummy"), temp);
        //end NIO

        InternalNioImplMavenCompiler compiler = InternalNioImplMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        InternalNioImplWorkspaceCompilationInfo info = new InternalNioImplWorkspaceCompilationInfo(tmp, compiler);
        InternalNioImplCompilationRequest req = new InternalNioImplDefaultCompilationRequest(info, new String[]{MavenArgs.VERSION}, new HashMap<>());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        Assert.assertTrue(res.getMavenOutput().isPresent());
        List<String> outPut = res.getMavenOutput().get();
        System.out.println("\nOutput from CompilerResponse>>>>\n::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
        for(String line : outPut){
            System.out.println(line);
        }
        System.out.println("::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::\nEnd output");

        InternalNioImplTestUtil.rm(tmpRoot.toFile());
    }



    @Test
    public void testHelpOutput() throws Exception {
        org.uberfire.java.nio.file.Path tmpRoot = org.uberfire.java.nio.file.Files.createTempDirectory("repo");
        org.uberfire.java.nio.file.Path tmp = org.uberfire.java.nio.file.Files.createDirectories(org.uberfire.java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        //NIO creation and copy content
        java.nio.file.Path temp = java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tmpRoot.toString(), "dummy"));
        TestUtil.copyTree(java.nio.file.Paths.get("src/test/projects/dummy"), temp);
        //end NIO

        InternalNioImplMavenCompiler compiler = InternalNioImplMavenCompilerFactory.getCompiler(mavenRepo, Decorator.NONE);
        Assert.assertTrue(compiler.isValid());

        InternalNioImplWorkspaceCompilationInfo info = new InternalNioImplWorkspaceCompilationInfo(tmp, compiler);
        InternalNioImplCompilationRequest req = new InternalNioImplDefaultCompilationRequest(info, new String[]{"-h"}, new HashMap<>());
        CompilationResponse res = compiler.compileSync(req);
        Assert.assertTrue(res.isSuccessful());
        Assert.assertTrue(res.getMavenOutput().isPresent());
        List<String> outPut = res.getMavenOutput().get();
        System.out.println("\nOutput from CompilerResponse>>>>\n::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
        for(String line : outPut){
            System.out.println(line);
        }
        System.out.println("::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::\nEnd output");

        InternalNioImplTestUtil.rm(tmpRoot.toFile());
    }

}
