package org.kie.workbench.common.services.backend.builder.compiler.external339;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;
import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieExecutionEventLogger extends AbstractExecutionListener {

    private static final int LINE_LENGTH = 72;
    private static final int MAX_PADDED_BUILD_TIME_DURATION_LENGTH = 9;
    private static final int MAX_PROJECT_NAME_LENGTH = 52;
    private final Logger logger;

    public KieExecutionEventLogger() {
        this.logger = LoggerFactory.getLogger(org.apache.maven.cli.event.ExecutionEventLogger.class);
    }

    public KieExecutionEventLogger(Logger logger) {
        this.logger = (Logger) Validate.notNull(logger,
                                                "logger cannot be null",
                                                new Object[0]);
    }

    static String chars(char c,
                        int count) {
        StringBuilder buffer = new StringBuilder(count);

        for (int i = count; i > 0; --i) {
            buffer.append(c);
        }

        return buffer.toString();
    }

    public void projectDiscoveryStarted(ExecutionEvent event) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info("Scanning for projects...");
        }
    }

    public void sessionStarted(ExecutionEvent event) {
        if (this.logger.isInfoEnabled() && event.getSession().getProjects().size() > 1) {
            this.logger.info(chars('-',
                                   72));
            this.logger.info("Reactor Build Order:");
            this.logger.info("");
            Iterator i$ = event.getSession().getProjects().iterator();

            while (i$.hasNext()) {
                MavenProject project = (MavenProject) i$.next();
                this.logger.info(project.getName());
            }
        }
    }

    public void sessionEnded(ExecutionEvent event) {
        if (this.logger.isInfoEnabled()) {
            if (event.getSession().getProjects().size() > 1) {
                this.logReactorSummary(event.getSession());
            }

            this.logResult(event.getSession());
            this.logStats(event.getSession());
            this.logger.info(chars('-',
                                   72));
        }
    }

    void logReactorSummary(MavenSession session) {
        this.logger.info(chars('-',
                               72));
        this.logger.info("Reactor Summary:");
        this.logger.info("");
        MavenExecutionResult result = session.getResult();

        StringBuilder buffer;
        for (Iterator i$ = session.getProjects().iterator(); i$.hasNext(); this.logger.info(buffer.toString())) {
            MavenProject project = (MavenProject) i$.next();
            buffer = new StringBuilder(128);
            buffer.append(project.getName());
            buffer.append(' ');
            if (buffer.length() <= 52) {
                while (buffer.length() < 52) {
                    buffer.append('.');
                }

                buffer.append(' ');
            }

            BuildSummary buildSummary = result.getBuildSummary(project);
            if (buildSummary == null) {
                buffer.append("SKIPPED");
            } else {
                String buildTimeDuration;
                int padSize;
                if (buildSummary instanceof BuildSuccess) {
                    buffer.append("SUCCESS [");
                    buildTimeDuration = CLIReportingUtils.formatDuration(buildSummary.getTime());
                    padSize = 9 - buildTimeDuration.length();
                    if (padSize > 0) {
                        buffer.append(chars(' ',
                                            padSize));
                    }

                    buffer.append(buildTimeDuration);
                    buffer.append("]");
                } else if (buildSummary instanceof BuildFailure) {
                    buffer.append("FAILURE [");
                    buildTimeDuration = CLIReportingUtils.formatDuration(buildSummary.getTime());
                    padSize = 9 - buildTimeDuration.length();
                    if (padSize > 0) {
                        buffer.append(chars(' ',
                                            padSize));
                    }

                    buffer.append(buildTimeDuration);
                    buffer.append("]");
                }
            }
        }
    }

    void logResult(MavenSession session) {
        this.logger.info(chars('-',
                               72));
        if (session.getResult().hasExceptions()) {
            this.logger.info("BUILD FAILURE");
        } else {
            this.logger.info("BUILD SUCCESS");
        }
    }

    void logStats(MavenSession session) {
        this.logger.info(chars('-',
                               72));
        long finish = System.currentTimeMillis();
        long time = finish - session.getRequest().getStartTime().getTime();
        String wallClock = session.getRequest().getDegreeOfConcurrency() > 1 ? " (Wall Clock)" : "";
        this.logger.info("Total time: " + CLIReportingUtils.formatDuration(time) + wallClock);
        this.logger.info("Finished at: " + CLIReportingUtils.formatTimestamp(finish));
        System.gc();
        Runtime r = Runtime.getRuntime();
        long mb = 1048576L;
        this.logger.info("Final Memory: " + (r.totalMemory() - r.freeMemory()) / mb + "M/" + r.totalMemory() / mb + "M");
    }

    public void projectSkipped(ExecutionEvent event) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info(chars(' ',
                                   72));
            this.logger.info(chars('-',
                                   72));
            this.logger.info("Skipping " + event.getProject().getName());
            this.logger.info("This project has been banned from the build due to previous failures.");
            this.logger.info(chars('-',
                                   72));
        }
    }

    public void projectStarted(ExecutionEvent event) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info(chars(' ',
                                   72));
            this.logger.info(chars('-',
                                   72));
            this.logger.info("Building " + event.getProject().getName() + " " + event.getProject().getVersion());
            this.logger.info(chars('-',
                                   72));
        }
    }

    public void mojoSkipped(ExecutionEvent event) {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn("Goal " + event.getMojoExecution().getGoal() + " requires online mode for execution but Maven is currently offline, skipping");
        }
    }

    public void mojoStarted(ExecutionEvent event) {
        if (this.logger.isInfoEnabled()) {
            StringBuilder buffer = new StringBuilder(128);
            buffer.append("--- ");
            this.append(buffer,
                        event.getMojoExecution());
            this.append(buffer,
                        event.getProject());
            buffer.append(" ---");
            this.logger.info("");
            this.logger.info(buffer.toString());
        }
    }

    public void forkStarted(ExecutionEvent event) {
        if (this.logger.isInfoEnabled()) {
            StringBuilder buffer = new StringBuilder(128);
            buffer.append(">>> ");
            this.append(buffer,
                        event.getMojoExecution());
            buffer.append(" > ");
            this.appendForkInfo(buffer,
                                event.getMojoExecution().getMojoDescriptor());
            this.append(buffer,
                        event.getProject());
            buffer.append(" >>>");
            this.logger.info("");
            this.logger.info(buffer.toString());
        }
    }

    public void forkSucceeded(ExecutionEvent event) {
        if (this.logger.isInfoEnabled()) {
            StringBuilder buffer = new StringBuilder(128);
            buffer.append("<<< ");
            this.append(buffer,
                        event.getMojoExecution());
            buffer.append(" < ");
            this.appendForkInfo(buffer,
                                event.getMojoExecution().getMojoDescriptor());
            this.append(buffer,
                        event.getProject());
            buffer.append(" <<<");
            this.logger.info("");
            this.logger.info(buffer.toString());
        }
    }

    void append(StringBuilder buffer,
                MojoExecution me) {
        buffer.append(me.getArtifactId()).append(':').append(me.getVersion());
        buffer.append(':').append(me.getGoal());
        if (me.getExecutionId() != null) {
            buffer.append(" (").append(me.getExecutionId()).append(')');
        }
    }

    void appendForkInfo(StringBuilder buffer,
                        MojoDescriptor md) {
        if (StringUtils.isNotEmpty(md.getExecutePhase())) {
            if (StringUtils.isNotEmpty(md.getExecuteLifecycle())) {
                buffer.append('[');
                buffer.append(md.getExecuteLifecycle());
                buffer.append(']');
            }

            buffer.append(md.getExecutePhase());
        } else {
            buffer.append(':');
            buffer.append(md.getExecuteGoal());
        }
    }

    void append(StringBuilder buffer,
                MavenProject project) {
        buffer.append(" @ ").append(project.getArtifactId());
    }

    public void forkedProjectStarted(ExecutionEvent event) {
        if (this.logger.isInfoEnabled() && event.getMojoExecution().getForkedExecutions().size() > 1) {
            this.logger.info(chars(' ',
                                   72));
            this.logger.info(chars('>',
                                   72));
            this.logger.info("Forking " + event.getProject().getName() + " " + event.getProject().getVersion());
            this.logger.info(chars('>',
                                   72));
        }
    }
}
