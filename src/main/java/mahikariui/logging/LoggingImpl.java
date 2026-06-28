/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.logging;

import java.util.List;
import mahikariui.utils.StringUtils;

public abstract class LoggingImpl {
    private final String loggerName;
    boolean appendName;
    private boolean debugMode;

    public LoggingImpl(String loggerName, boolean debug, boolean appendName) {
        this.loggerName = loggerName;
        this.debugMode = debug;
        this.appendName = appendName;
    }

    public LoggingImpl(String loggerName, boolean debug) {
        this(loggerName, debug, false);
    }

    public LoggingImpl(String loggerName) {
        this(loggerName, false);
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void error(String logMessage, Object ... logArguments) {
        throw new UnsupportedOperationException();
    }

    public void error(String logMessage, Throwable ex) {
        this.error(logMessage + "\n" + StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void error(Throwable ex) {
        this.error(StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void warn(String logMessage, Object ... logArguments) {
        throw new UnsupportedOperationException();
    }

    public void warn(String logMessage, Throwable ex) {
        this.warn(logMessage + "\n" + StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void warn(Throwable ex) {
        this.warn(StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void info(String logMessage, Object ... logArguments) {
        throw new UnsupportedOperationException();
    }

    public void info(String logMessage, Throwable ex) {
        this.info(logMessage + "\n" + StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void info(Throwable ex) {
        this.info(StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void debugInfo(String logMessage, Object ... logArguments) {
        if (this.isDebugMode()) {
            this.info("[  MAHIKARIUI ] - [DEBUG] " + logMessage, logArguments);
        }
    }

    public void debugInfo(String logMessage, Throwable ex) {
        this.debugInfo(logMessage + "\n" + StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void debugInfo(Throwable ex) {
        this.debugInfo(StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void debugWarn(String logMessage, Object ... logArguments) {
        if (this.isDebugMode()) {
            this.warn("[ MAHIKARIUI ] - [DEBUG] " + logMessage, logArguments);
        }
    }

    public void debugWarn(String logMessage, Throwable ex) {
        this.debugWarn(logMessage + "\n" + StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void debugWarn(Throwable ex) {
        this.debugWarn(StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void debugError(String logMessage, Object ... logArguments) {
        if (this.isDebugMode()) {
            this.error("[ MAHIKARIUI ] - [DEBUG] " + logMessage, logArguments);
        }
    }

    public void debugError(String logMessage, Throwable ex) {
        this.debugError(logMessage + "\n" + StringUtils.getStackTrace(ex), new Object[0]);
    }

    public void debugError(Throwable ex) {
        this.debugError(StringUtils.getStackTrace(ex), new Object[0]);
    }

    public String parse(String message, Object ... args) {
        String prefix;
        String string = prefix = this.appendName ? this.loggerName + ": " : "";
        if (args == null || args.length == 0) {
            return prefix + StringUtils.normalize(message);
        }
        try {
            return prefix + StringUtils.normalize(String.format(message, args));
        }
        catch (Exception e) {
            return prefix + "[Log Format Error] " + message;
        }
    }

    public void printStackTrace(Throwable ex, boolean showLogging, String prefix, String verbosePrefix, Appendable ... outputs) {
        List<String> splitEx = StringUtils.splitTextByNewLine(StringUtils.getStackTrace(ex));
        if (outputs != null) {
            for (Appendable output : outputs) {
                if (output == null) continue;
                try {
                    if (showLogging) {
                        for (String line : splitEx) {
                            line = line.replace("\t", "    ");
                            output.append(line).append('\n');
                        }
                        continue;
                    }
                    output.append(splitEx.getFirst()).append('\n');
                    if (splitEx.size() <= 1) continue;
                    output.append('\n').append(verbosePrefix).append('\n');
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
        if (showLogging) {
            this.error(prefix, new Object[0]);
            this.error(ex);
        } else {
            this.error("%1$s \"%2$s\"", prefix, splitEx.getFirst());
            if (splitEx.size() > 1) {
                this.error(verbosePrefix, new Object[0]);
            }
        }
    }

    public void printStackTrace(Throwable ex, String prefix, String verbosePrefix, Appendable ... outputs) {
        this.printStackTrace(ex, this.isDebugMode(), prefix, verbosePrefix, outputs);
    }
}

