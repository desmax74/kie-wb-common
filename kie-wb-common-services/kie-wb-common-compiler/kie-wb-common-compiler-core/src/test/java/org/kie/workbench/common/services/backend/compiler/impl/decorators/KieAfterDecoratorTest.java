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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;

import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.BaseCompilerTest;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;
import org.kie.workbench.common.services.backend.compiler.impl.BaseMavenCompiler;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.impl.kie.KieCompilationResponse;
import org.uberfire.java.nio.file.Path;

public class KieAfterDecoratorTest extends BaseCompilerTest {

    public KieAfterDecoratorTest() {
        super("target/test-classes/kjar-2-single-resources");
    }

    @Test
    public void compileTest() {

        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);

        KieAfterDecorator decorator = new KieAfterDecorator(new BaseMavenCompiler());
        KieCompilationResponse kieRes = (KieCompilationResponse) decorator.compile(req);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(kieRes.isSuccessful()).isTrue();
            softly.assertThat(kieRes.getMavenOutput()).isEmpty();
            softly.assertThat(kieRes.getKieModule()).isNotNull();
            softly.assertThat(kieRes.getKieModuleMetaInfo()).isNotNull();
        });
    }

    @Test
    public void compileWithOverrideTest() throws Exception {

        Map<Path, InputStream> override = new HashMap<>();
        org.uberfire.java.nio.file.Path path = org.uberfire.java.nio.file.Paths.get(tmpRoot + "/dummy/src/main/java/org/kie/maven/plugin/test/Person.java");
        InputStream input = new FileInputStream(new File("target/test-classes/kjar-2-single-resources_override/src/main/java/org/kie/maven/plugin/test/Person.java"));
        override.put(path, input);

        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);

        KieAfterDecorator decorator = new KieAfterDecorator(new BaseMavenCompiler());
        KieCompilationResponse kieRes = (KieCompilationResponse) decorator.compile(req, override);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(kieRes.isSuccessful()).isTrue();
            softly.assertThat(kieRes.getMavenOutput()).isEmpty();
            softly.assertThat(kieRes.getKieModule()).isNotNull();
            softly.assertThat(kieRes.getKieModuleMetaInfo()).isNotNull();
        });
    }
}
