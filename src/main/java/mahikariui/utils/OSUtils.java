/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.utils;

public class OSUtils {
    public static final String USER_DIR = System.getProperty("user.dir");
    public static final String OS_NAME = System.getProperty("os.name");
    public static final String OS_ARCH = System.getProperty("os.arch");
    public static final boolean IS_64_BIT = OS_ARCH.contains("amd64") || OS_ARCH.contains("x86_64");
    public static final float JAVA_SPEC = Float.parseFloat(System.getProperty("java.specification.version"));
}

