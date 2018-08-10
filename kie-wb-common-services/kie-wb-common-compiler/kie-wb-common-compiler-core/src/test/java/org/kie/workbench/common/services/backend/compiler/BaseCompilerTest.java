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
package org.kie.workbench.common.services.backend.compiler;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.kie.workbench.common.services.backend.utils.TestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

public class BaseCompilerTest {

    protected static Path tmpRoot;
    protected String mavenRepo;
    protected static Logger logger = LoggerFactory.getLogger(BaseCompilerTest.class);
    protected String alternateSettingsAbsPath;
    protected WorkspaceCompilationInfo info;
    protected AFCompiler compiler;
    protected KieCompilationResponse res;
    private static int gitDaemonPort;



    public static int findFreePort() {
        int port = 0;
        try {
            ServerSocket server = new ServerSocket(0);
            port = server.getLocalPort();
            server.close();
        } catch (IOException e) {
            Assert.fail("Can't find free port!");
        }
        logger.debug("Found free port " + port);
        return port;
    }


    @BeforeClass
    public static void setupSystemProperties() {
        String gitPort = System.getProperty("org.uberfire.nio.git.daemon.port");
        if(gitPort!= null) {
            gitDaemonPort = Integer.valueOf(gitPort);
        }
        int freePort = findFreePort();
        System.setProperty("org.uberfire.nio.git.daemon.port", String.valueOf(freePort));
        logger.info("Git port used:{}",freePort);
    }

    @Rule
    public TestName testName = new TestName();

    public BaseCompilerTest(String prjName) {
        try {
            mavenRepo = TestUtilMaven.getMavenRepo();
            tmpRoot = Files.createTempDirectory("repo");
            alternateSettingsAbsPath = TestUtilMaven.getSettingsFile();
            Path tmp = TestUtil.createAndCopyToDirectory(tmpRoot, "dummy", prjName);
            info = new WorkspaceCompilationInfo(Paths.get(tmp.toUri()));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public BaseCompilerTest(String prjName, KieDecorator decorator) {
        this(prjName);
        try {
            compiler = KieMavenCompilerFactory.getCompiler(decorator);
            CompilationRequest req = new DefaultCompilationRequest(mavenRepo,
                                                                   info,
                                                                   new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                                   Boolean.FALSE);
            res = (KieCompilationResponse) compiler.compile(req);
            TestUtil.saveMavenLogIfCompilationResponseNotSuccessfull(tmpRoot, res, this.getClass(), testName);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    protected WorkspaceCompilationInfo createdNewPrjInRepo(String dirName, String prjName) throws IOException {
        Path tmp = TestUtil.createAndCopyToDirectory(tmpRoot, dirName, prjName);
        return new WorkspaceCompilationInfo(Paths.get(tmp.toUri()));
    }

    @AfterClass
    public static void tearDown() {
        System.setProperty("org.uberfire.nio.git.daemon.port", String.valueOf(gitDaemonPort));
        if (tmpRoot != null) {
            TestUtil.rm(tmpRoot.toFile());
        }
    }
}
