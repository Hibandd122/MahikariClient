/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package mahikariui.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import mahikariui.logging.LoggingImpl;

public class ApacheLogger
extends LoggingImpl {
    private final Logger logInstance;

    public ApacheLogger(String loggerName, boolean debug) {
        super(loggerName, debug);
        this.logInstance = LogManager.getLogger((String)loggerName);
    }

    public ApacheLogger(String loggerName) {
        this(loggerName, false);
    }

    public Logger getLogInstance() {
        return this.logInstance;
    }

    @Override
    public void error(String logMessage, Object ... logArguments) {
        this.getLogInstance().error(this.parse(logMessage, logArguments));
    }

    @Override
    public void warn(String logMessage, Object ... logArguments) {
        this.getLogInstance().warn(this.parse(logMessage, logArguments));
    }

    @Override
    public void info(String logMessage, Object ... logArguments) {
        this.getLogInstance().info(this.parse(logMessage, logArguments));
    }
}

