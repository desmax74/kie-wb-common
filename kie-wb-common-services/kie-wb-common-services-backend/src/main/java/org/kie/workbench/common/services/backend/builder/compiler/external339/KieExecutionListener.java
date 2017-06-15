package org.kie.workbench.common.services.backend.builder.compiler.external339;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.cli.CLIReportingUtils;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

public class KieExecutionListener extends AbstractExecutionListener {

    private List<String> accumulator;
    private ExecutionListener executionListener;

    public KieExecutionListener(ExecutionListener executionListener) {
        super();
        this.executionListener = executionListener;
        accumulator = new ArrayList<>();
    }

    public KieExecutionListener(ExecutionListener executionListener,
                                List<String> mavenOutput) {
        super();
        this.executionListener = executionListener;
        accumulator = mavenOutput;
    }

    private static String chars(char c,
                                int count) {
        StringBuilder buffer = new StringBuilder(count);

        for (int i = count; i > 0; --i) {
            buffer.append(c);
        }

        return buffer.toString();
    }

    private void append(ExecutionEvent event) {
        accumulator.add(event.toString());
    }

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        accumulator.add("Scanning for projects...");
        executionListener.projectDiscoveryStarted(event);
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        if (event.getSession().getProjects().size() > 1) {
            logReactorSummary(event.getSession());
        }

        this.logResult(event.getSession());
        this.logStats(event.getSession());
        accumulator.add((chars('-',
                               72)));
        executionListener.sessionEnded(event);
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        append(event);
        executionListener.projectSucceeded(event);
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        append(event);
        executionListener.projectFailed(event);
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        append(event);
        executionListener.forkStarted(event);
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        append(event);
        executionListener.forkSucceeded(event);
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        append(event);
        executionListener.forkFailed(event);
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        accumulator.add("Goal " + event.getMojoExecution().getGoal() + " requires online mode for execution but Maven is currently offline, skipping");
        executionListener.mojoSkipped(event);
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append("--- ");
        this.append(buffer,
                    event.getMojoExecution());
        this.append(buffer,
                    event.getProject());
        buffer.append(" ---");
        accumulator.add("");
        accumulator.add(buffer.toString());
        executionListener.mojoStarted(event);
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        append(event);
        executionListener.mojoSucceeded(event);
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        append(event);
        executionListener.mojoFailed(event);
    }

    @Override
    public void forkedProjectStarted(ExecutionEvent event) {
        append(event);
        executionListener.forkedProjectStarted(event);
    }

    @Override
    public void forkedProjectSucceeded(ExecutionEvent event) {
        append(event);
        executionListener.forkedProjectSucceeded(event);
    }

    @Override
    public void forkedProjectFailed(ExecutionEvent event) {
        append(event);
        executionListener.forkedProjectFailed(event);
    }

    public List<String> getAccumulator() {
        return accumulator;
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        accumulator.add(chars('-',
                              72));
        accumulator.add("Reactor Build Order:");
        accumulator.add("");
        Iterator i$ = event.getSession().getProjects().iterator();

        while (i$.hasNext()) {
            MavenProject project = (MavenProject) i$.next();
            accumulator.add(project.getName());
        }
        executionListener.sessionStarted(event);
    }

    void append(StringBuilder buffer,
                MavenProject project) {
        buffer.append(" @ ").append(project.getArtifactId());
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        accumulator.add(chars(' ',
                              72));
        accumulator.add(chars('-',
                              72));
        accumulator.add("Skipping " + event.getProject().getName());
        accumulator.add("This project has been banned from the build due to previous failures.");
        accumulator.add(chars('-',
                              72));
        executionListener.projectSkipped(event);
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        accumulator.add(chars(' ',
                              72));
        accumulator.add(chars('-',
                              72));
        accumulator.add("Building " + event.getProject().getName() + " " + event.getProject().getVersion());
        accumulator.add(chars('-',
                              72));
        executionListener.projectStarted(event);
    }

    void logResult(MavenSession session) {
        accumulator.add(chars('-',
                              72));
        if (session.getResult().hasExceptions()) {
            accumulator.add("BUILD FAILURE");
        } else {
            accumulator.add("BUILD SUCCESS");
        }
    }

    void logStats(MavenSession session) {
        accumulator.add(chars('-',
                              72));
        long finish = System.currentTimeMillis();
        long time = finish - session.getRequest().getStartTime().getTime();
        String wallClock = session.getRequest().getDegreeOfConcurrency() > 1 ? " (Wall Clock)" : "";
        accumulator.add("Total time: " + CLIReportingUtils.formatDuration(time) + wallClock);
        accumulator.add("Finished at: " + CLIReportingUtils.formatTimestamp(finish));
        System.gc();
        Runtime r = Runtime.getRuntime();
        long mb = 1048576L;
        accumulator.add("Final Memory: " + (r.totalMemory() - r.freeMemory()) / mb + "M/" + r.totalMemory() / mb + "M");
    }

    void logReactorSummary(MavenSession session) {
        accumulator.add(chars('-',
                              72));
        accumulator.add("Reactor Summary:");
        accumulator.add("");
        MavenExecutionResult result = session.getResult();

        StringBuilder buffer;
        for (Iterator i$ = session.getProjects().iterator(); i$.hasNext(); accumulator.add(buffer.toString())) {
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

    private void append(StringBuilder buffer,
                        MojoExecution me) {
        buffer.append(me.getArtifactId()).append(':').append(me.getVersion());
        buffer.append(':').append(me.getGoal());
        if (me.getExecutionId() != null) {
            buffer.append(" (").append(me.getExecutionId()).append(')');
        }
    }
}
