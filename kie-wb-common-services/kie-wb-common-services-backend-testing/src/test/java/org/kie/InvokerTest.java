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
package org.kie;

import org.apache.maven.shared.invoker.*;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;


public class InvokerTest {

    @Test
    public void testWithInvoker() throws Exception {
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File("/home/garfield/dev/maven-3.3.9"));
        //invoker.setLocalRepositoryDirectory(new File());

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("src/test/projects/dummy/pom.xml"));
        request.setGoals(Arrays.asList("compile"));
        request.setShowErrors(Boolean.TRUE);
        request.setBatchMode(Boolean.TRUE);


        InvocationResult result = invoker.execute(request);

        if (result.getExitCode() != 0) {
            if (result.getExecutionException() != null) {
                throw new Exception("Failed to publish site.",
                        result.getExecutionException());
            } else {
                throw new Exception("Failed to publish site. Exit code: " +
                        result.getExitCode());
            }
        }
    }
}
