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

package org.kie.workbench.common.services.backend.builder.compiler.external339;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.maven.BuildAbort;
import org.apache.maven.InternalErrorException;
import org.apache.maven.Maven;
import org.apache.maven.building.FileSource;
import org.apache.maven.building.Problem;
import org.apache.maven.building.Source;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.cli.event.DefaultEventSpyContext;
import org.apache.maven.cli.event.ExecutionEventLogger;
import org.apache.maven.cli.internal.BootstrapCoreExtensionManager;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.internal.extension.model.io.xpp3.CoreExtensionsXpp3Reader;
import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.cli.logging.Slf4jConfigurationFactory;
import org.apache.maven.cli.logging.Slf4jLoggerManager;
import org.apache.maven.cli.logging.Slf4jStdoutLogger;
import org.apache.maven.cli.transfer.ConsoleMavenTransferListener;
import org.apache.maven.cli.transfer.QuietMavenTransferListener;
import org.apache.maven.cli.transfer.Slf4jMavenTransferListener;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.*;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.properties.internal.SystemProperties;
import org.apache.maven.toolchain.building.DefaultToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuilder;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;
import org.codehaus.plexus.*;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.transfer.TransferListener;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

/**
 * Modified from Maven to permit builds without installations and var envs.
 */
public class KieMavenCli {

    public static final String MULTIMODULE_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory";
    public static final String userHome = System.getProperty("user.home");
    public static final Path userMavenConfigurationHome = Paths.get(userHome, ".m2");
    private static final Logger logger = LoggerFactory.getLogger(KieMavenCli.class);
    private static final String EXT_CLASS_PATH = "maven.ext.class.path";

    private static final String EXTENSIONS_FILENAME = ".mvn/extensions.xml";

    private LoggerManager plexusLoggerManager;

    private ILoggerFactory slf4jLoggerFactory;

    private Logger slf4jLogger;

    private EventSpyDispatcher eventSpyDispatcher;

    private ModelProcessor modelProcessor;

    private Maven maven;

    private MavenExecutionRequestPopulator executionRequestPopulator;

    private ToolchainsBuilder toolchainsBuilder;

    private DefaultSecDispatcher dispatcher;

    private Map<String, KieConfigurationProcessor> configurationProcessors;

    public KieMavenCli() {
    }

    private static <T> List<T> reverse(List<T> list) {
        List<T> copy = new ArrayList<T>(list);
        Collections.reverse(copy);
        return copy;
    }

    static File resolveFile(File file, String workingDirectory) {
        if (file == null) {
            return null;
        } else if (file.isAbsolute()) {
            return file;
        } else if (file.getPath().startsWith(File.separator)) {
            // drive-relative Windows path
            return file.getAbsoluteFile();
        } else {
            return new File(workingDirectory, file.getPath()).getAbsoluteFile();
        }
    }

    static Path resolvePath(Path file, String workingDirectory) {
        return file == null ? null : (file.isAbsolute() ? file : (file.getFileName().startsWith(File.separator) ? file.toAbsolutePath() : (Paths.get(workingDirectory, file.getFileName().toString()))));
    }

    static void populateProperties(CommandLine commandLine, Properties systemProperties, Properties userProperties) {
        EnvironmentUtils.addEnvVars(systemProperties);

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        if (commandLine.hasOption(CLIManager.SET_SYSTEM_PROPERTY)) {
            String[] defStrs = commandLine.getOptionValues(CLIManager.SET_SYSTEM_PROPERTY);

            if (defStrs != null) {
                for (String defStr : defStrs) {
                    setCliProperty(defStr, userProperties);
                }
            }
        }

        SystemProperties.addSystemProperties(systemProperties);

        // ----------------------------------------------------------------------
        // Properties containing info about the currently running version of Maven
        // These override any corresponding properties set on the command line
        // ----------------------------------------------------------------------

        Properties buildProperties = KieCLIReportingUtils.getBuildProperties();

        String mavenVersion = buildProperties.getProperty(KieCLIReportingUtils.BUILD_VERSION_PROPERTY);
        systemProperties.setProperty("maven.version", mavenVersion);

        String mavenBuildVersion = KieCLIReportingUtils.createMavenVersionString(buildProperties);
        systemProperties.setProperty("maven.build.version", mavenBuildVersion);
    }

    protected static void setCliProperty(String property, Properties properties) {
        String name;

        String value;

        int i = property.indexOf("=");

        if (i <= 0) {
            name = property.trim();

            value = "true";
        } else {
            name = property.substring(0, i).trim();

            value = property.substring(i + 1);
        }

        properties.setProperty(name, value);
        System.setProperty(name, value);
    }

    public int doMain(KieCliRequest cliRequest) {
        PlexusContainer localContainer = null;
        try {
            initialize(cliRequest);
            cli(cliRequest);
            logging(cliRequest);
            version(cliRequest);
            properties(cliRequest);
            localContainer = container(cliRequest);
            commands(cliRequest);
            configure(cliRequest);//@TODO
            toolchains(cliRequest);
            populateRequest(cliRequest);
            repository(cliRequest);
            return execute(cliRequest);
        } catch (ExitException e) {
            return e.exitCode;
        } catch (UnrecognizedOptionException e) {
            return 1;
        } catch (BuildAbort e) {
            KieCLIReportingUtils.showError(slf4jLogger, "ABORTED", e, cliRequest.isShowErrors());
            return 2;
        } catch (Exception e) {
            e.getStackTrace();
            KieCLIReportingUtils.showError(slf4jLogger, "Error executing Maven.", e, cliRequest.isShowErrors());

            return 1;
        } finally {
            if (localContainer != null) {
                localContainer.dispose();
            }
        }
    }

    protected void initialize(KieCliRequest cliRequest)
            throws ExitException {
        if (cliRequest.getWorkingDirectory() == null) {
            cliRequest.setWorkingDirectory(System.getProperty("user.dir"));
        }

        if (cliRequest.getMultiModuleProjectDirectory() == null) {
            String basedirProperty = System.getProperty(MULTIMODULE_PROJECT_DIRECTORY);
            if (basedirProperty == null) {
                System.err.format("-D%s system propery is not set."
                        + " Check $M2_HOME environment variable and mvn script match.", MULTIMODULE_PROJECT_DIRECTORY);
                throw new ExitException(1);
            }
            Path basedir = basedirProperty != null ? Paths.get(basedirProperty) : Paths.get("");
            cliRequest.setMultiModuleProjectDirectory(basedir.toAbsolutePath().toString());
        }

    }

    protected void cli(KieCliRequest cliRequest)
            throws Exception {
        //
        // Parsing errors can happen during the processing of the arguments and we prefer not having to check if
        // the logger is null and construct this so we can use an SLF4J logger everywhere.
        //
        slf4jLogger = new Slf4jStdoutLogger();

        CLIManager cliManager = new CLIManager();

        List<String> args = new ArrayList<String>();

        try {
            Path configFile = Paths.get(cliRequest.getMultiModuleProjectDirectory(), ".mvn/maven.config");

            if (java.nio.file.Files.isRegularFile(configFile)) {
                for (String arg : Files.toString(configFile.toFile(), Charsets.UTF_8).split("\\s+")) {
                    args.add(arg);
                }

                CommandLine config = cliManager.parse(args.toArray(new String[args.size()]));
                List<?> unrecongized = config.getArgList();
                if (!unrecongized.isEmpty()) {
                    throw new ParseException("Unrecognized maven.config entries: " + unrecongized);
                }
            }
        } catch (ParseException e) {
            System.err.println("Unable to parse maven.config: " + e.getMessage());
            cliManager.displayHelp(System.out);
            throw e;
        }

        try {
            args.addAll(0, Arrays.asList(cliRequest.getArgs()));
            cliRequest.setCommandLine(cliManager.parse(args.toArray(new String[args.size()])));
        } catch (ParseException e) {
            System.err.println("Unable to parse command line options: " + e.getMessage());
            cliManager.displayHelp(System.out);
            throw e;
        }

        if (cliRequest.getCommandLine().hasOption(CLIManager.HELP)) {
            cliManager.displayHelp(System.out);
            throw new ExitException(0);
        }

        if (cliRequest.getCommandLine().hasOption(CLIManager.VERSION)) {
            System.out.println(KieCLIReportingUtils.showVersion());
            throw new ExitException(0);
        }
    }

    protected void logging(KieCliRequest cliRequest) {
        cliRequest.setDebug(cliRequest.getCommandLine().hasOption(CLIManager.DEBUG));
        cliRequest.setQuiet(!cliRequest.isDebug() && cliRequest.getCommandLine().hasOption(CLIManager.QUIET));
        cliRequest.setShowErrors(cliRequest.isDebug() || cliRequest.getCommandLine().hasOption(CLIManager.ERRORS));

        slf4jLoggerFactory = LoggerFactory.getILoggerFactory();
        Slf4jConfiguration slf4jConfiguration = Slf4jConfigurationFactory.getConfiguration(slf4jLoggerFactory);

        if (cliRequest.isDebug()) {
            cliRequest.getRequest().setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_DEBUG);
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.DEBUG);
        } else if (cliRequest.isQuiet()) {
            cliRequest.getRequest().setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_ERROR);
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.ERROR);
        }

        if (cliRequest.getCommandLine().hasOption(CLIManager.LOG_FILE)) {
            File logFile = new File(cliRequest.getCommandLine().getOptionValue(CLIManager.LOG_FILE));
            logFile = resolveFile(logFile, cliRequest.getWorkingDirectory());

            try {
                PrintStream ps = new PrintStream(new FileOutputStream(logFile));
                System.setOut(ps);
                System.setErr(ps);
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
            }
        }

        slf4jConfiguration.activate();

        plexusLoggerManager = new Slf4jLoggerManager();
        slf4jLogger = slf4jLoggerFactory.getLogger(this.getClass().getName());
    }

    protected void version(KieCliRequest cliRequest) {
        if (cliRequest.isDebug() || cliRequest.getCommandLine().hasOption(CLIManager.SHOW_VERSION)) {
            System.out.println(KieCLIReportingUtils.showVersion());
        }
    }

    protected void commands(KieCliRequest cliRequest) {
        if (cliRequest.isShowErrors()) {
            slf4jLogger.info("Error stacktraces are turned on.");
        }

        if (MavenExecutionRequest.CHECKSUM_POLICY_WARN.equals(cliRequest.getRequest().getGlobalChecksumPolicy())) {
            slf4jLogger.info("Disabling strict checksum verification on all artifact downloads.");
        } else if (MavenExecutionRequest.CHECKSUM_POLICY_FAIL.equals(cliRequest.getRequest().getGlobalChecksumPolicy())) {
            slf4jLogger.info("Enabling strict checksum verification on all artifact downloads.");
        }
    }

    protected void properties(KieCliRequest cliRequest) {
        populateProperties(cliRequest.getCommandLine(), cliRequest.getSystemProperties(), cliRequest.getUserProperties());
    }

    protected PlexusContainer container(KieCliRequest cliRequest)
            throws Exception {
        if (cliRequest.getClassWorld() == null) {
            cliRequest.setClassWorld(new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader()));
        }

        ClassRealm coreRealm = cliRequest.getClassWorld().getClassRealm("plexus.core");
        if (coreRealm == null) {
            coreRealm = cliRequest.getClassWorld().getRealms().iterator().next();
        }

        List<File> extClassPath = parseExtClasspath(cliRequest);

        CoreExtensionEntry coreEntry = CoreExtensionEntry.discoverFrom(coreRealm);
        List<CoreExtensionEntry> extensions =
                loadCoreExtensions(cliRequest, coreRealm, coreEntry.getExportedArtifacts());

        ClassRealm containerRealm = setupContainerRealm(cliRequest.getClassWorld(), coreRealm, extClassPath, extensions);

        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(cliRequest.getClassWorld())
                .setRealm(containerRealm)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setName("maven");

        Set<String> exportedArtifacts = new HashSet<String>(coreEntry.getExportedArtifacts());
        Set<String> exportedPackages = new HashSet<String>(coreEntry.getExportedPackages());
        for (CoreExtensionEntry extension : extensions) {
            exportedArtifacts.addAll(extension.getExportedArtifacts());
            exportedPackages.addAll(extension.getExportedPackages());
        }

        final CoreExports exports = new CoreExports(containerRealm, exportedArtifacts, exportedPackages);

        DefaultPlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ILoggerFactory.class).toInstance(slf4jLoggerFactory);
                bind(CoreExports.class).toInstance(exports);
            }
        });

        //@MAX
        container.addComponent(cliRequest.getMap(), HashMap.class, "kieMap");
        //Object item = container.lookup(Map.class, "java.util.HashMap", "kieMap");

        // NOTE: To avoid inconsistencies, we'll use the TCCL exclusively for lookups
        container.setLookupRealm(null);

        container.setLoggerManager(plexusLoggerManager);

        for (CoreExtensionEntry extension : extensions) {
            container.discoverComponents(extension.getClassRealm());
        }

        customizeContainer(container);

        container.getLoggerManager().setThresholds(cliRequest.getRequest().getLoggingLevel());

        Thread.currentThread().setContextClassLoader(container.getContainerRealm());

        eventSpyDispatcher = container.lookup(EventSpyDispatcher.class);

        DefaultEventSpyContext eventSpyContext = new DefaultEventSpyContext();
        Map<String, Object> data = eventSpyContext.getData();
        data.put("plexus", container);
        data.put("workingDirectory", cliRequest.getWorkingDirectory());
        data.put("systemProperties", cliRequest.getSystemProperties());
        data.put("userProperties", cliRequest.getUserProperties());
        data.put("versionProperties", KieCLIReportingUtils.getBuildProperties());

        eventSpyDispatcher.init(eventSpyContext);

        slf4jLogger = slf4jLoggerFactory.getLogger(this.getClass().getName());

        maven = container.lookup(Maven.class);

        executionRequestPopulator = container.lookup(MavenExecutionRequestPopulator.class);

        modelProcessor = createModelProcessor(container);

        configurationProcessors = container.lookupMap(KieConfigurationProcessor.class);


        toolchainsBuilder = container.lookup(ToolchainsBuilder.class);

        dispatcher = (DefaultSecDispatcher) container.lookup(SecDispatcher.class, "maven");

        return container;
    }

    protected List<CoreExtensionEntry> loadCoreExtensions(KieCliRequest cliRequest, ClassRealm containerRealm,
                                                          Set<String> providedArtifacts) {
        if (cliRequest.getMultiModuleProjectDirectory() == null) {
            return Collections.emptyList();
        }

        Path extensionsFile = Paths.get(cliRequest.getMultiModuleProjectDirectory().toString(), EXTENSIONS_FILENAME);
        if (!java.nio.file.Files.isRegularFile(extensionsFile)) {
            return Collections.emptyList();
        }

        try {
            List<CoreExtension> extensions = readCoreExtensionsDescriptor(extensionsFile);
            if (extensions.isEmpty()) {
                return Collections.emptyList();
            }

            ContainerConfiguration cc = new DefaultContainerConfiguration() //
                    .setClassWorld(cliRequest.getClassWorld()) //
                    .setRealm(containerRealm) //
                    .setClassPathScanning(PlexusConstants.SCANNING_INDEX) //
                    .setAutoWiring(true) //
                    .setName("maven");

            DefaultPlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ILoggerFactory.class).toInstance(slf4jLoggerFactory);
                }
            });

            /*container.addComponent(cliRequest.getMap(), HashMap.class,"kieMap");
            Object item =  container.lookup(Map.class,"java.util.HashMap","kieMap");
            System.out.println("item:"+item.toString());
            System.out.println("Container in cli 3:"+container.toString());*/

            try {
                container.setLookupRealm(null);

                container.setLoggerManager(plexusLoggerManager);

                container.getLoggerManager().setThresholds(cliRequest.getRequest().getLoggingLevel());

                Thread.currentThread().setContextClassLoader(container.getContainerRealm());

                executionRequestPopulator = container.lookup(MavenExecutionRequestPopulator.class);

                configurationProcessors = container.lookupMap(KieConfigurationProcessor.class);

                configure(cliRequest);

                MavenExecutionRequest request = DefaultMavenExecutionRequest.copy(cliRequest.getRequest());

                request = populateRequest(cliRequest, request);

                request = executionRequestPopulator.populateDefaults(request);

                BootstrapCoreExtensionManager resolver = container.lookup(BootstrapCoreExtensionManager.class);

                return resolver.loadCoreExtensions(request, providedArtifacts, extensions);
            } finally {
                executionRequestPopulator = null;
                container.dispose();
            }
        } catch (RuntimeException e) {
            // runtime exceptions are most likely bugs in maven, let them bubble up to the user
            throw e;
        } catch (Exception e) {
            slf4jLogger.warn("Failed to read extensions descriptor " + extensionsFile + ": " + e.getMessage());
        }
        return Collections.emptyList();
    }

    protected List<CoreExtension> readCoreExtensionsDescriptor(Path extensionsFile)
            throws IOException, XmlPullParserException {
        CoreExtensionsXpp3Reader parser = new CoreExtensionsXpp3Reader();
        InputStream is = null;
        try {
            is = new BufferedInputStream(new ByteArrayInputStream(java.nio.file.Files.readAllBytes(extensionsFile)));
            return parser.read(is).getExtensions();
        } finally {
            IOUtil.close(is);
        }
    }

    protected ClassRealm setupContainerRealm(ClassWorld classWorld, ClassRealm coreRealm, List<File> extClassPath,
                                             List<CoreExtensionEntry> extensions)
            throws Exception {
        if (!extClassPath.isEmpty() || !extensions.isEmpty()) {
            ClassRealm extRealm = classWorld.newRealm("maven.ext", null);

            extRealm.setParentRealm(coreRealm);

            slf4jLogger.debug("Populating class realm " + extRealm.getId());

            for (File file : extClassPath) {
                slf4jLogger.debug("  Included " + file);

                extRealm.addURL(file.toURI().toURL());
            }

            for (CoreExtensionEntry entry : reverse(extensions)) {
                Set<String> exportedPackages = entry.getExportedPackages();
                ClassRealm realm = entry.getClassRealm();
                for (String exportedPackage : exportedPackages) {
                    extRealm.importFrom(realm, exportedPackage);
                }
                if (exportedPackages.isEmpty()) {
                    // sisu uses realm imports to establish component visibility
                    extRealm.importFrom(realm, realm.getId());
                }
            }

            return extRealm;
        }

        return coreRealm;
    }

    protected List<File> parseExtClasspath(KieCliRequest cliRequest) {
        String extClassPath = cliRequest.getUserProperties().getProperty(EXT_CLASS_PATH);
        if (extClassPath == null) {
            extClassPath = cliRequest.getSystemProperties().getProperty(EXT_CLASS_PATH);
        }

        List<File> jars = new ArrayList<File>();

        if (StringUtils.isNotEmpty(extClassPath)) {
            for (String jar : StringUtils.split(extClassPath, File.pathSeparator)) {
                File file = resolveFile(new File(jar), cliRequest.getWorkingDirectory());

                slf4jLogger.debug("  Included " + file);

                jars.add(file);
            }
        }

        return jars;
    }

    protected void repository(KieCliRequest cliRequest)
            throws Exception {
        if (cliRequest.getCommandLine().hasOption(CLIManager.LEGACY_LOCAL_REPOSITORY)
                || Boolean.getBoolean("maven.legacyLocalRepo")) {
            cliRequest.getRequest().setUseLegacyLocalRepository(true);
        }
    }

    protected int execute(KieCliRequest cliRequest) throws MavenExecutionRequestPopulationException {
        MavenExecutionRequest request = executionRequestPopulator.populateDefaults(cliRequest.getRequest());

        eventSpyDispatcher.onEvent(request);

        MavenExecutionResult result = maven.execute(request);

        eventSpyDispatcher.onEvent(result);

        eventSpyDispatcher.close();

        if (result.hasExceptions()) {
            ExceptionHandler handler = new DefaultExceptionHandler();

            Map<String, String> references = new LinkedHashMap<String, String>();

            MavenProject project = null;

            for (Throwable exception : result.getExceptions()) {
                ExceptionSummary summary = handler.handleException(exception);

                logSummary(summary, references, "", cliRequest.isShowErrors());

                if (project == null && exception instanceof LifecycleExecutionException) {
                    project = ((LifecycleExecutionException) exception).getProject();
                }
            }

            slf4jLogger.error("");

            if (!cliRequest.isShowErrors()) {
                slf4jLogger.error("To see the full stack trace of the errors, re-run Maven with the -e switch.");
            }
            if (!slf4jLogger.isDebugEnabled()) {
                slf4jLogger.error("Re-run Maven using the -X switch to enable full debug logging.");
            }

            if (!references.isEmpty()) {
                slf4jLogger.error("");
                slf4jLogger.error("For more information about the errors and possible solutions"
                        + ", please read the following articles:");

                for (Map.Entry<String, String> entry : references.entrySet()) {
                    slf4jLogger.error(entry.getValue() + " " + entry.getKey());
                }
            }

            if (project != null && !project.equals(result.getTopologicallySortedProjects().get(0))) {
                slf4jLogger.error("");
                slf4jLogger.error("After correcting the problems, you can resume the build with the command");
                slf4jLogger.error("  mvn <goals> -rf :" + project.getArtifactId());
            }

            if (MavenExecutionRequest.REACTOR_FAIL_NEVER.equals(cliRequest.getRequest().getReactorFailureBehavior())) {
                slf4jLogger.info("Build failures were ignored.");

                return 0;
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }

    protected void logSummary(ExceptionSummary summary, Map<String, String> references, String indent,
                              boolean showErrors) {
        String referenceKey = "";

        if (StringUtils.isNotEmpty(summary.getReference())) {
            referenceKey = references.get(summary.getReference());
            if (referenceKey == null) {
                referenceKey = "[Help " + (references.size() + 1) + "]";
                references.put(summary.getReference(), referenceKey);
            }
        }

        String msg = summary.getMessage();

        if (StringUtils.isNotEmpty(referenceKey)) {
            if (msg.indexOf('\n') < 0) {
                msg += " -> " + referenceKey;
            } else {
                msg += "\n-> " + referenceKey;
            }
        }

        String[] lines = msg.split("(\r\n)|(\r)|(\n)");

        for (int i = 0; i < lines.length; i++) {
            String line = indent + lines[i].trim();

            if ((i == lines.length - 1)
                    && (showErrors || (summary.getException() instanceof InternalErrorException))) {
                slf4jLogger.error(line, summary.getException());
            } else {
                slf4jLogger.error(line);
            }
        }

        indent += "  ";

        for (ExceptionSummary child : summary.getChildren()) {
            logSummary(child, references, indent, showErrors);
        }
    }

    protected void configure(KieCliRequest cliRequest)
            throws Exception {

        cliRequest.getRequest().setEventSpyDispatcher(eventSpyDispatcher);

        int userSuppliedConfigurationProcessorCount = configurationProcessors.size() - 1;

        if (userSuppliedConfigurationProcessorCount == 0) {

            configurationProcessors.get(KieConfigurationProcessor.HINT).process(cliRequest);
        } else if (userSuppliedConfigurationProcessorCount == 1) {

            for (Entry<String, KieConfigurationProcessor> entry : configurationProcessors.entrySet()) {
                String hint = entry.getKey();
                if (!hint.equals(KieConfigurationProcessor.HINT)) {
                    KieConfigurationProcessor configurationProcessor = entry.getValue();
                    configurationProcessor.process(cliRequest);
                }
            }
        } else if (userSuppliedConfigurationProcessorCount > 1) {
            StringBuffer sb = new StringBuffer(
                    String.format("\nThere can only be one user supplied ConfigurationProcessor, there are %s:\n\n",
                            userSuppliedConfigurationProcessorCount));
            for (Entry<String, KieConfigurationProcessor> entry : configurationProcessors.entrySet()) {
                String hint = entry.getKey();
                if (!hint.equals(KieConfigurationProcessor.HINT)) {
                    KieConfigurationProcessor configurationProcessor = entry.getValue();
                    sb.append(String.format("%s\n", configurationProcessor.getClass().getName()));
                }
            }
            sb.append(String.format("\n"));
            throw new Exception(sb.toString());
        }
    }

    @SuppressWarnings("checkstyle:methodlength")
    protected void toolchains(KieCliRequest cliRequest)
            throws Exception {
        Path userToolchainsFile;

        if (cliRequest.getCommandLine().hasOption(CLIManager.ALTERNATE_USER_TOOLCHAINS)) {
            userToolchainsFile = Paths.get(cliRequest.getCommandLine().getOptionValue(CLIManager.ALTERNATE_USER_TOOLCHAINS));
            userToolchainsFile = resolvePath(userToolchainsFile, cliRequest.getWorkingDirectory());

            if (!java.nio.file.Files.isRegularFile(userToolchainsFile)) {
                throw new FileNotFoundException("The specified user toolchains file does not exist: " + userToolchainsFile);
            }
        } else {
            userToolchainsFile = Paths.get(userMavenConfigurationHome.toString(), "toolchains.xml");
        }

        Path globalToolchainsFile;

        if (cliRequest.getCommandLine().hasOption(CLIManager.ALTERNATE_GLOBAL_TOOLCHAINS)) {
            globalToolchainsFile =
                    Paths.get(cliRequest.getCommandLine().getOptionValue(CLIManager.ALTERNATE_GLOBAL_TOOLCHAINS));
            globalToolchainsFile = resolvePath(globalToolchainsFile, cliRequest.getWorkingDirectory());

            if (!java.nio.file.Files.isRegularFile(globalToolchainsFile)) {
                throw new FileNotFoundException("The specified global toolchains file does not exist: "
                        + globalToolchainsFile);
            }
        } else {
            globalToolchainsFile = Paths.get(userMavenConfigurationHome.toString(), "toolchains.xml");
        }

        cliRequest.getRequest().setGlobalToolchainsFile(globalToolchainsFile.toFile());
        cliRequest.getRequest().setUserToolchainsFile(userToolchainsFile.toFile());

        DefaultToolchainsBuildingRequest toolchainsRequest = new DefaultToolchainsBuildingRequest();
        if (java.nio.file.Files.isRegularFile(globalToolchainsFile)) {
            toolchainsRequest.setGlobalToolchainsSource(new FileSource(globalToolchainsFile.toFile()));
        }
        if (java.nio.file.Files.isRegularFile(userToolchainsFile)) {
            toolchainsRequest.setUserToolchainsSource(new FileSource(userToolchainsFile.toFile()));
        }

        eventSpyDispatcher.onEvent(toolchainsRequest);

        slf4jLogger.debug("Reading global toolchains from "
                + getLocation(toolchainsRequest.getGlobalToolchainsSource(), globalToolchainsFile));
        slf4jLogger.debug("Reading user toolchains from "
                + getLocation(toolchainsRequest.getUserToolchainsSource(), userToolchainsFile));

        ToolchainsBuildingResult toolchainsResult = toolchainsBuilder.build(toolchainsRequest);

        eventSpyDispatcher.onEvent(toolchainsRequest);

        executionRequestPopulator.populateFromToolchains(cliRequest.getRequest(),
                toolchainsResult.getEffectiveToolchains());

        if (!toolchainsResult.getProblems().isEmpty() && slf4jLogger.isWarnEnabled()) {
            slf4jLogger.warn("");
            slf4jLogger.warn("Some problems were encountered while building the effective toolchains");

            for (Problem problem : toolchainsResult.getProblems()) {
                slf4jLogger.warn(problem.getMessage() + " @ " + problem.getLocation());
            }

            slf4jLogger.warn("");
        }
    }

    protected Object getLocation(Source source, Path defaultLocation) {
        if (source != null) {
            return source.getLocation();
        }
        return defaultLocation.toString();
    }

    protected MavenExecutionRequest populateRequest(KieCliRequest cliRequest) {
        return populateRequest(cliRequest, cliRequest.getRequest());
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    protected MavenExecutionRequest populateRequest(KieCliRequest cliRequest, MavenExecutionRequest request) {
        CommandLine commandLine = cliRequest.getCommandLine();
        String workingDirectory = cliRequest.getWorkingDirectory();
        boolean quiet = cliRequest.isQuiet();
        boolean showErrors = cliRequest.isShowErrors();

        String[] deprecatedOptions = {"up", "npu", "cpu", "npr"};
        for (String deprecatedOption : deprecatedOptions) {
            if (commandLine.hasOption(deprecatedOption)) {
                slf4jLogger.warn("Command line option -" + deprecatedOption
                        + " is deprecated and will be removed in future Maven versions.");
            }
        }

        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        if (commandLine.hasOption(CLIManager.BATCH_MODE)) {
            request.setInteractiveMode(false);
        }

        boolean noSnapshotUpdates = false;
        if (commandLine.hasOption(CLIManager.SUPRESS_SNAPSHOT_UPDATES)) {
            noSnapshotUpdates = true;
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        @SuppressWarnings("unchecked")
        List<String> goals = commandLine.getArgList();

        boolean recursive = true;

        // this is the default behavior.
        String reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_FAST;

        if (commandLine.hasOption(CLIManager.NON_RECURSIVE)) {
            recursive = false;
        }

        if (commandLine.hasOption(CLIManager.FAIL_FAST)) {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_FAST;
        } else if (commandLine.hasOption(CLIManager.FAIL_AT_END)) {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_AT_END;
        } else if (commandLine.hasOption(CLIManager.FAIL_NEVER)) {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_NEVER;
        }

        if (commandLine.hasOption(CLIManager.OFFLINE)) {
            request.setOffline(true);
        }

        boolean updateSnapshots = false;

        if (commandLine.hasOption(CLIManager.UPDATE_SNAPSHOTS)) {
            updateSnapshots = true;
        }

        String globalChecksumPolicy = null;

        if (commandLine.hasOption(CLIManager.CHECKSUM_FAILURE_POLICY)) {
            globalChecksumPolicy = MavenExecutionRequest.CHECKSUM_POLICY_FAIL;
        } else if (commandLine.hasOption(CLIManager.CHECKSUM_WARNING_POLICY)) {
            globalChecksumPolicy = MavenExecutionRequest.CHECKSUM_POLICY_WARN;
        }

        File baseDirectory = new File(workingDirectory, "").getAbsoluteFile();

        // ----------------------------------------------------------------------
        // Profile Activation
        // ----------------------------------------------------------------------

        List<String> activeProfiles = new ArrayList<String>();

        List<String> inactiveProfiles = new ArrayList<String>();

        if (commandLine.hasOption(CLIManager.ACTIVATE_PROFILES)) {
            String[] profileOptionValues = commandLine.getOptionValues(CLIManager.ACTIVATE_PROFILES);
            if (profileOptionValues != null) {
                for (String profileOptionValue : profileOptionValues) {
                    StringTokenizer profileTokens = new StringTokenizer(profileOptionValue, ",");

                    while (profileTokens.hasMoreTokens()) {
                        String profileAction = profileTokens.nextToken().trim();

                        if (profileAction.startsWith("-") || profileAction.startsWith("!")) {
                            inactiveProfiles.add(profileAction.substring(1));
                        } else if (profileAction.startsWith("+")) {
                            activeProfiles.add(profileAction.substring(1));
                        } else {
                            activeProfiles.add(profileAction);
                        }
                    }
                }
            }
        }

        TransferListener transferListener;

        if (quiet) {
            transferListener = new QuietMavenTransferListener();
        } else if (request.isInteractiveMode() && !cliRequest.getCommandLine().hasOption(CLIManager.LOG_FILE)) {
            //
            // If we're logging to a file then we don't want the console transfer listener as it will spew
            // download progress all over the place
            //
            transferListener = getConsoleTransferListener();
        } else {
            transferListener = getBatchTransferListener();
        }

        ExecutionListener executionListener = new ExecutionEventLogger();
        if (eventSpyDispatcher != null) {
            executionListener = eventSpyDispatcher.chainListener(executionListener);
        }

        String alternatePomFile = null;
        if (commandLine.hasOption(CLIManager.ALTERNATE_POM_FILE)) {
            alternatePomFile = commandLine.getOptionValue(CLIManager.ALTERNATE_POM_FILE);
        }

        request.setBaseDirectory(baseDirectory).setGoals(goals)
                .setSystemProperties(cliRequest.getSystemProperties())
                .setUserProperties(cliRequest.getUserProperties())
                .setReactorFailureBehavior(reactorFailureBehaviour) // default: fail fast
                .setRecursive(recursive) // default: true
                .setShowErrors(showErrors) // default: false
                .addActiveProfiles(activeProfiles) // optional
                .addInactiveProfiles(inactiveProfiles) // optional
                .setExecutionListener(executionListener)
                .setTransferListener(transferListener) // default: batch mode which goes along with interactive
                .setUpdateSnapshots(updateSnapshots) // default: false
                .setNoSnapshotUpdates(noSnapshotUpdates) // default: false
                .setGlobalChecksumPolicy(globalChecksumPolicy) // default: warn
                .setMultiModuleProjectDirectory(new File(cliRequest.getMultiModuleProjectDirectory()));

        if (alternatePomFile != null) {
            File pom = resolveFile(new File(alternatePomFile.trim()), workingDirectory);
            if (pom.isDirectory()) {
                pom = new File(pom, "pom.xml");
            }

            request.setPom(pom);
        } else if (modelProcessor != null) {
            File pom = modelProcessor.locatePom(baseDirectory);

            if (pom.isFile()) {
                request.setPom(pom);
            }
        }

        if ((request.getPom() != null) && (request.getPom().getParentFile() != null)) {
            request.setBaseDirectory(request.getPom().getParentFile());
        }

        if (commandLine.hasOption(CLIManager.RESUME_FROM)) {
            request.setResumeFrom(commandLine.getOptionValue(CLIManager.RESUME_FROM));
        }

        if (commandLine.hasOption(CLIManager.PROJECT_LIST)) {
            String[] projectOptionValues = commandLine.getOptionValues(CLIManager.PROJECT_LIST);

            List<String> inclProjects = new ArrayList<String>();
            List<String> exclProjects = new ArrayList<String>();

            if (projectOptionValues != null) {
                for (String projectOptionValue : projectOptionValues) {
                    StringTokenizer projectTokens = new StringTokenizer(projectOptionValue, ",");

                    while (projectTokens.hasMoreTokens()) {
                        String projectAction = projectTokens.nextToken().trim();

                        if (projectAction.startsWith("-") || projectAction.startsWith("!")) {
                            exclProjects.add(projectAction.substring(1));
                        } else if (projectAction.startsWith("+")) {
                            inclProjects.add(projectAction.substring(1));
                        } else {
                            inclProjects.add(projectAction);
                        }
                    }
                }
            }

            request.setSelectedProjects(inclProjects);
            request.setExcludedProjects(exclProjects);
        }

        if (commandLine.hasOption(CLIManager.ALSO_MAKE)
                && !commandLine.hasOption(CLIManager.ALSO_MAKE_DEPENDENTS)) {
            request.setMakeBehavior(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);
        } else if (!commandLine.hasOption(CLIManager.ALSO_MAKE)
                && commandLine.hasOption(CLIManager.ALSO_MAKE_DEPENDENTS)) {
            request.setMakeBehavior(MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM);
        } else if (commandLine.hasOption(CLIManager.ALSO_MAKE)
                && commandLine.hasOption(CLIManager.ALSO_MAKE_DEPENDENTS)) {
            request.setMakeBehavior(MavenExecutionRequest.REACTOR_MAKE_BOTH);
        }

        String localRepoProperty = request.getUserProperties().getProperty(MavenCli.LOCAL_REPO_PROPERTY);

        if (localRepoProperty == null) {
            localRepoProperty = request.getSystemProperties().getProperty(MavenCli.LOCAL_REPO_PROPERTY);
        }

        if (localRepoProperty != null) {
            request.setLocalRepositoryPath(localRepoProperty);
        }

        request.setCacheNotFound(true);
        request.setCacheTransferError(false);

        //
        // Builder, concurrency and parallelism
        //
        // We preserve the existing methods for builder selection which is to look for various inputs in the threading
        // configuration. We don't have an easy way to allow a pluggable builder to provide its own configuration
        // parameters but this is sufficient for now. Ultimately we want components like Builders to provide a way to
        // extend the command line to accept its own configuration parameters.
        //
        final String threadConfiguration = commandLine.hasOption(CLIManager.THREADS)
                ? commandLine.getOptionValue(CLIManager.THREADS)
                : request.getSystemProperties().getProperty(
                MavenCli.THREADS_DEPRECATED); // TODO: Remove this setting. Note that the int-tests use it

        if (threadConfiguration != null) {
            //
            // Default to the standard multithreaded builder
            //
            request.setBuilderId("multithreaded");

            if (threadConfiguration.contains("C")) {
                request.setDegreeOfConcurrency(calculateDegreeOfConcurrencyWithCoreMultiplier(threadConfiguration));
            } else {
                request.setDegreeOfConcurrency(Integer.valueOf(threadConfiguration));
            }
        }

        //
        // Allow the builder to be overriden by the user if requested. The builders are now pluggable.
        //
        if (commandLine.hasOption(CLIManager.BUILDER)) {
            request.setBuilderId(commandLine.getOptionValue(CLIManager.BUILDER));
        }

        return request;
    }

    protected int calculateDegreeOfConcurrencyWithCoreMultiplier(String threadConfiguration) {
        int procs = Runtime.getRuntime().availableProcessors();
        return (int) (Float.valueOf(threadConfiguration.replace("C", "")) * procs);
    }

    protected TransferListener getConsoleTransferListener() {
        return new ConsoleMavenTransferListener(System.out);
        //return new ConsoleMavenTransferListener(System.out);
    }

    //
    // Customizations available via the CLI
    //

    protected TransferListener getBatchTransferListener() {
        return new Slf4jMavenTransferListener();
    }

    protected void customizeContainer(PlexusContainer container) {
    }

    protected ModelProcessor createModelProcessor(PlexusContainer container)
            throws ComponentLookupException {
        return container.lookup(ModelProcessor.class);
    }

    static class ExitException extends Exception {
        public int exitCode;

        public ExitException(int exitCode) {
            this.exitCode = exitCode;
        }
    }
}
