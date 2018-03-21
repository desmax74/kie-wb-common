package org.kie.workbench.common.services.backend.compiler.invoker;

import org.apache.maven.shared.invoker.InvokerLogger;
import org.kie.workbench.common.services.backend.logback.OutputSharedMap;

public class KieInvokerLogger implements InvokerLogger {

    private int threshold;

    private void addMsg(int level, String s, Throwable error) {

        if (level > threshold) {
            return;
        }

        if (s == null && error == null) {
            // don't log when there's nothing to log.
            return;
        }

        switch (level) {
            case (DEBUG):
                append("[DEBUG]", s);
                break;

            case (INFO):
                append("[INFO]", s);
                break;

            case (WARN):
                append("[WARN]", s);
                break;

            case (ERROR):
                append("[ERROR]", s);
                break;

            case (FATAL):
                append("[FATAL]", s);
                break;

            default:
        }
    }

    private void append(String level, String s) {
        OutputSharedMap.addMsgToLog(Thread.currentThread().getName(), level + s);
    }

    @Override
    public void debug(String s) {
        addMsg(DEBUG, s, null);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        addMsg(DEBUG, s, throwable);
    }

    @Override
    public void info(String s) {
        addMsg(INFO, s, null);
    }

    @Override
    public void info(String s, Throwable throwable) {
        addMsg(INFO, s, throwable);
    }

    @Override
    public void warn(String s) {
        addMsg(WARN, s, null);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        addMsg(WARN, s, throwable);
    }

    @Override
    public void error(String s) {
        addMsg(ERROR, s, null);
    }

    @Override
    public void error(String s, Throwable throwable) {
        addMsg(ERROR, s, throwable);
    }

    @Override
    public void fatalError(String s) {
        addMsg(FATAL, s, null);
    }

    @Override
    public void fatalError(String s, Throwable throwable) {
        addMsg(FATAL, s, throwable);
    }

    public boolean isDebugEnabled() {
        return threshold >= DEBUG;
    }

    public boolean isErrorEnabled() {
        return threshold >= ERROR;
    }

    public boolean isFatalErrorEnabled() {
        return threshold >= FATAL;
    }

    public boolean isInfoEnabled() {
        return threshold >= INFO;
    }

    public boolean isWarnEnabled() {
        return threshold >= WARN;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
