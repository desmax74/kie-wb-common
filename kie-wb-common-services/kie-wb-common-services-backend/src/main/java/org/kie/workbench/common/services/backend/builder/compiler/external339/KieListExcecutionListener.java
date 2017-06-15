package org.kie.workbench.common.services.backend.builder.compiler.external339;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;

public class KieExceutionListener extends AbstractExecutionListener {

    private List<String> accumulator;
    private StringBuffer sb;

    private void append(ExecutionEvent event){
        accumulator.add(event.toString());
        sb.append(event.toString());
    }


    public KieExceutionListener() {
        super();
        accumulator = new ArrayList<>();
        sb = new StringBuffer();
    }


    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void forkedProjectStarted(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void forkedProjectSucceeded(ExecutionEvent event) {
        append(event);
    }

    @Override
    public void forkedProjectFailed(ExecutionEvent event) {
        append(event);
    }

    public List<String> getListAccumulator(){
        return accumulator;
    }

    public StringBuffer getStringBufferAccumulator(){
        return sb;
    }
}
