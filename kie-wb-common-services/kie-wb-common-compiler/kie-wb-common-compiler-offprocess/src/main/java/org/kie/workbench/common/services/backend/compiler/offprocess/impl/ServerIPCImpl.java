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
package org.kie.workbench.common.services.backend.compiler.offprocess.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ExcerptAppender;
import org.kie.workbench.common.services.backend.compiler.AFCompiler;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.configuration.KieDecorator;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultKieCompilationResponseOffProcess;
import org.kie.workbench.common.services.backend.compiler.impl.WorkspaceCompilationInfo;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieMavenCompilerFactory;
import org.uberfire.java.nio.file.Paths;

public class ServerIPCImpl {

    public static void main(String[] args) throws Exception {
        String uuid = args[0];
        String workingDir = args[1];
        String mavenRepo = args[2];
        String alternateSettingsAbsPath = args[3];
        String queueName = args[4];
        String threadName = Thread.currentThread().getName();
        QueueProvider provider = new QueueProvider(queueName);
        execute(workingDir, mavenRepo, alternateSettingsAbsPath, uuid, provider);
        Thread.currentThread().setName(threadName);// restore the previous name to avoid the override of the maven output
    }

    public static void execute(String workingDir, String mavenRepo, String alternateSettingsAbsPath, String uuid, QueueProvider provider) throws Exception {
        DefaultKieCompilationResponseOffProcess res = build(workingDir, mavenRepo, alternateSettingsAbsPath, uuid);
        byte[] bytez = serialize(res);
        if (bytez == null) {
            return;
        }
        writeOnQueue(bytez, provider);
    }

    private static void writeOnQueue(byte[] bytez, QueueProvider provider) {
        ExcerptAppender appender = provider.getQueue().acquireAppender();
        appender.writeBytes(Bytes.allocateDirect(bytez));
    }

    private static DefaultKieCompilationResponseOffProcess build(String prjPath, String mavenRepo, String alternateSettingsAbsPath, String uuid) {
        AFCompiler compiler = KieMavenCompilerFactory.getCompiler(KieDecorator.KIE_AND_LOG_AFTER);
        WorkspaceCompilationInfo info = new WorkspaceCompilationInfo(Paths.get(prjPath));
        CompilationRequest req;
        if (alternateSettingsAbsPath != null && alternateSettingsAbsPath.length() > 1) {
            req = new DefaultCompilationRequest(mavenRepo,
                                                info,
                                                new String[]{
                                                        MavenCLIArgs.DEPENDENCY_RESOLVE,
                                                        MavenCLIArgs.COMPILE,
                                                        MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath
                                                },
                                                Boolean.FALSE,
                                                uuid);
        } else {
            req = new DefaultCompilationRequest(mavenRepo,
                                                info,
                                                new String[]{
                                                        MavenCLIArgs.DEPENDENCY_RESOLVE,
                                                        MavenCLIArgs.COMPILE
                                                },
                                                Boolean.FALSE,
                                                uuid);
        }
        KieCompilationResponse res = (KieCompilationResponse) compiler.compile(req);
        DefaultKieCompilationResponseOffProcess resConverted = new DefaultKieCompilationResponseOffProcess(res);
        return resConverted;
    }

    private static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }
}