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
package org.kie.workbench.common.services.backend.compiler.offprocess;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ChronicleQueueBuilder;

public class QueueProvider {

    private static ChronicleQueue queue;
    private static String prefixInfoObjs = "/offprocess-queue";

    static {
        init();
    }

    private static void init() {
        String basePath = System.getProperty("java.io.tmpdir") + prefixInfoObjs;
        queue = ChronicleQueueBuilder.single(basePath).build();
    }

    public static ChronicleQueue getQueue() {
        return queue;
    }

    public static void cleanQueue() {
        queue.close();
    }
}
