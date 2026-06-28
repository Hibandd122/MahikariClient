/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  edu.umd.cs.findbugs.annotations.SuppressFBWarnings
 */
package mahikariui.logging;


import java.util.logging.Logger;
import mahikariui.logging.LoggingImpl;
import mahikariui.utils.FileUtils;


public class JavaLogger
extends LoggingImpl {
    private final Logger logInstance;

    public JavaLogger(String loggerName, boolean debug) {
        super(loggerName, debug);
        Logger logger = null;
        try {
            logger = (Logger)FileUtils.loadClass("net.minecraft.src.ModLoader", "ModLoader").getDeclaredMethod("getLogger", new Class[0]).invoke(null, new Object[0]);
            if (logger != null) {
                this.appendName = true;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.logInstance = logger != null ? logger : Logger.getLogger(loggerName);
    }

    public JavaLogger(String loggerName) {
        this(loggerName, false);
    }

    public Logger getLogInstance() {
        return this.logInstance;
    }

    @Override
    public void error(String logMessage, Object ... logArguments) {
        this.getLogInstance().severe(this.parse(logMessage, logArguments));
    }

    @Override
    public void warn(String logMessage, Object ... logArguments) {
        this.getLogInstance().warning(this.parse(logMessage, logArguments));
    }

    @Override
    public void info(String logMessage, Object ... logArguments) {
        this.getLogInstance().info(this.parse(logMessage, logArguments));
    }
}

