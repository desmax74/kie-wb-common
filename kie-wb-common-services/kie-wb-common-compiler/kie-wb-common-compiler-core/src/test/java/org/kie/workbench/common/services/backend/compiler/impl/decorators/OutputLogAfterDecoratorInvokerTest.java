package org.kie.workbench.common.services.backend.compiler.impl.decorators;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.AfterClass;
import org.junit.Test;
import org.kie.workbench.common.services.backend.compiler.BaseCompilerTest;
import org.kie.workbench.common.services.backend.compiler.CompilationRequest;
import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.ResourcesConstants;
import org.kie.workbench.common.services.backend.compiler.configuration.MavenCLIArgs;

import org.kie.workbench.common.services.backend.compiler.impl.DefaultCompilationRequest;
import org.kie.workbench.common.services.backend.compiler.invoker.InvokerBaseMavenCompiler;
import org.uberfire.java.nio.file.Path;

public class OutputLogAfterDecoratorInvokerTest extends BaseCompilerTest {

    public OutputLogAfterDecoratorInvokerTest() {
        super(ResourcesConstants.DUMMY);
    }

    @AfterClass
    public static void tearDown() {
        BaseCompilerTest.tearDown();
    }

    @Test
    public void compileTest() {

        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);

        OutputLogAfterDecorator decorator = new OutputLogAfterDecorator(new InvokerBaseMavenCompiler());
        CompilationResponse res = decorator.compile(req);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(res.isSuccessful()).isTrue();
            softly.assertThat(res.getMavenOutput().size()).isGreaterThan(0);
        });
    }

    @Test
    public void compileWithOverrideTest() throws Exception {

        Map<Path, InputStream> override = new HashMap<>();
        org.uberfire.java.nio.file.Path path = org.uberfire.java.nio.file.Paths.get(tmpRoot + "/dummy/src/main/java/dummy/DummyOverride.java");
        InputStream input = new FileInputStream(new File("target/test-classes/dummy_override/src/main/java/dummy/DummyOverride.java"));
        override.put(path, input);

        CompilationRequest req = new DefaultCompilationRequest(mavenRepo.toAbsolutePath().toString(),
                                                               info,
                                                               new String[]{MavenCLIArgs.INSTALL, MavenCLIArgs.ALTERNATE_USER_SETTINGS + alternateSettingsAbsPath},
                                                               Boolean.FALSE);

        OutputLogAfterDecorator decorator = new OutputLogAfterDecorator(new InvokerBaseMavenCompiler());
        CompilationResponse res = decorator.compile(req, override);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(res.isSuccessful()).isTrue();
            softly.assertThat(res.getMavenOutput().size()).isGreaterThan(0);
        });
    }
}
