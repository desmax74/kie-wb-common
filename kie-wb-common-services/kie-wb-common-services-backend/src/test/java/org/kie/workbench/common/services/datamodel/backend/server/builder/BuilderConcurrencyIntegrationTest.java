/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.services.datamodel.backend.server.builder;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.guvnor.common.services.project.builder.model.BuildResults;
import org.junit.Test;
import org.kie.workbench.common.services.backend.builder.cache.ModuleCache;
import org.kie.workbench.common.services.backend.compiler.service.executors.CompilerLogLevel;
import org.kie.workbench.common.services.shared.project.KieModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.vfs.Path;
import org.uberfire.rpc.SessionInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.uberfire.backend.server.util.Paths.convert;

public class BuilderConcurrencyIntegrationTest extends AbstractWeldBuilderIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BuilderConcurrencyIntegrationTest.class);

    @Inject
    private ModuleCache moduleCache;

    @Test
    //https://bugzilla.redhat.com/show_bug.cgi?id=1145105
    public void testBuilderConcurrency() throws URISyntaxException {
        final URL pomUrl = this.getClass().getResource("/BuilderConcurrencyRepo/pom.xml");
        final org.uberfire.java.nio.file.Path nioPomPath = fs.getPath(pomUrl.toURI());
        final Path pomPath = convert(nioPomPath);

        final URL resourceUrl = this.getClass().getResource("/BuilderConcurrencyRepo/src/main/resources/update.drl");
        final org.uberfire.java.nio.file.Path nioResourcePath = fs.getPath(resourceUrl.toURI());
        final Path resourcePath = convert(nioResourcePath);

        final SessionInfo sessionInfo = mock(SessionInfo.class);

        //Force full build before attempting incremental changes
        final KieModule module = moduleService.resolveModule(resourcePath);
        final BuildResults buildResults = buildService.build(module);
        assertNotNull(buildResults);
        assertEquals(0,
                     buildResults.getErrorMessages().size());

        //Perform incremental build
        final int THREADS = 200;
        final Result result = new Result();
        ExecutorService es = Executors.newCachedThreadPool();
        for (int i = 0; i < THREADS; i++) {
            switch (i % 3) {
                case 0:
                    es.execute(() ->{
                            try {
                                logger.debug("Thread " + Thread.currentThread().getName() + " has started: BuildService.build( module )");
                                buildService.build(module);
                                logger.debug("Thread " + Thread.currentThread().getName() + " has completed.");
                            } catch (Throwable e) {
                                result.setFailed(true);
                                result.setMessage(e.getMessage());
                                logger.debug(e.getMessage());
                            }
                    });
                    break;
                case 1:
                    es.execute(() -> {
                        try {
                            logger.debug("Thread " + Thread.currentThread().getName() + " has started: LRUProjectDataModelOracleCache.invalidateProjectCache(...)");
                            logger.debug("Thread " + Thread.currentThread().getName() + " has completed.");
                        } catch (Throwable e) {
                            result.setFailed(true);
                            result.setMessage(e.getMessage());
                            logger.debug(e.getMessage());
                        }
                    });
                    break;
                default:
                    es.execute(() -> {
                            try {
                                logger.debug("Thread " + Thread.currentThread().getName() + " has started: LRUBuilderCache.assertBuilder( module ).getKieModuleIgnoringErrors();");
                                moduleCache.getOrCreateEntry(module).build(CompilerLogLevel.STANDARD);
                                logger.debug("Thread " + Thread.currentThread().getName() + " has completed.");
                            } catch (Throwable e) {
                                result.setFailed(true);
                                result.setMessage(e.getMessage());
                                logger.debug(e.getMessage());
                            }
                    });
            }
        }

        es.shutdown();
        try {
            es.awaitTermination(5,
                                TimeUnit.MINUTES);
        } catch (InterruptedException e) {
        }
        if (result.isFailed()) {
            fail(result.getMessage());
        }
    }

    private static class Result {

        private boolean failed = false;
        private String message = "";

        public synchronized boolean isFailed() {
            return failed;
        }

        public synchronized void setFailed(boolean failed) {
            this.failed = failed;
        }

        public synchronized String getMessage() {
            return message;
        }

        public synchronized void setMessage(String message) {
            this.message = message;
        }
    }
}