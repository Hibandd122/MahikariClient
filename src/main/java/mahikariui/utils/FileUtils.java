/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.GsonBuilder
 */
package mahikariui.utils;

import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import mahikariui.core.Constants;
import mahikariui.impl.Pair;
import mahikariui.utils.MappingUtils;
import mahikariui.utils.OSUtils;
import mahikariui.utils.StringUtils;
import mahikariui.utils.UrlUtils;

public class FileUtils {
    private static final GsonBuilder GSON_BUILDER = new GsonBuilder();
    private static final Map<String, Class<?>> CLASS_CACHE = StringUtils.newHashMap();
    private static final Map<String, Pair<ScheduledExecutorService, ThreadFactory>> THREAD_FACTORY_MAP = StringUtils.newHashMap();
    public static final ClassLoader CLASS_LOADER = Thread.currentThread().getContextClassLoader();

    public static Pair<ScheduledExecutorService, ThreadFactory> getOrCreateScheduler(String name) {
        if (!THREAD_FACTORY_MAP.containsKey(name)) {
            ThreadFactory threadFactory = r -> {
                Thread t = new Thread(r, name);
                t.setDaemon(true);
                return t;
            };
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(threadFactory);
            THREAD_FACTORY_MAP.put(name, new Pair<ScheduledExecutorService, ThreadFactory>(exec, threadFactory));
        }
        return THREAD_FACTORY_MAP.get(name);
    }

    public static ScheduledExecutorService getThreadPool(String name) {
        return FileUtils.getOrCreateScheduler(name).getFirst();
    }

    public static String getFileExtension(File file) {
        return FileUtils.getFileExtension(file.getName());
    }

    public static String getFileExtension(String name) {
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf);
    }

    public static Class<?> getValidClass(ClassLoader loader, boolean init, boolean forceCache, String ... paths) {
        List<String> classList = StringUtils.newArrayList(paths);
        for (String path : paths) {
            StringUtils.addEntriesNotPresent(classList, MappingUtils.getUnmappedClassesMatching(path, true));
        }
        Iterator<String> iterator = classList.iterator();
        while (iterator.hasNext()) {
            Class<?> result;
            String path;
            switch (path = (String)iterator.next()) {
                case "boolean": {
                    return Boolean.TYPE;
                }
                case "byte": {
                    return Byte.TYPE;
                }
                case "short": {
                    return Short.TYPE;
                }
                case "int": {
                    return Integer.TYPE;
                }
                case "long": {
                    return Long.TYPE;
                }
                case "float": {
                    return Float.TYPE;
                }
                case "double": {
                    return Double.TYPE;
                }
                case "char": {
                    return Character.TYPE;
                }
                case "void": {
                    return Void.TYPE;
                }
            }
            if (!CLASS_CACHE.containsKey(path) || forceCache) {
                result = null;
                try {
                    result = Class.forName(path, init, loader);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                CLASS_CACHE.put(path, result);
            }
            if ((result = CLASS_CACHE.get(path)) == null) continue;
            return result;
        }
        return null;
    }

    public static String fileToString(File file, String encoding) throws Exception {
        return FileUtils.fileToString(Files.newInputStream(file.toPath(), new OpenOption[0]), encoding);
    }

    public static String fileToString(InputStream stream, String encoding) throws Exception {
        return UrlUtils.readerToString(new BufferedReader(new InputStreamReader(stream, Charset.forName(encoding))));
    }

    public static <T> T getJsonFromURL(String url, String encoding, Class<T> targetClass, Modifiers ... args) throws Exception {
        return FileUtils.getJsonData(new URI(url), encoding, targetClass, args);
    }

    public static <T> T getJsonFromURL(String url, Class<T> targetClass, Modifiers ... args) throws Exception {
        return FileUtils.getJsonFromURL(url, "UTF-8", targetClass, args);
    }

    public static <T> T getJsonData(URI url, String encoding, Class<T> targetClass, Modifiers ... args) throws Exception {
        return (T)FileUtils.getJsonData(UrlUtils.getURLText(url, encoding), targetClass, args);
    }

    public static <T> T getJsonData(URI url, Class<T> targetClass, Modifiers ... args) throws Exception {
        return FileUtils.getJsonData(url, "UTF-8", targetClass, args);
    }

    public static <T> T getJsonData(File data, String encoding, Class<T> classObj, Modifiers ... args) throws Exception {
        return (T)FileUtils.getJsonData(FileUtils.fileToString(data, encoding), classObj, args);
    }

    public static <T> T getJsonData(File data, Class<T> classObj, Modifiers ... args) throws Exception {
        return FileUtils.getJsonData(data, "UTF-8", classObj, args);
    }

    public static <T> T getJsonData(String data, Class<T> classObj, Modifiers ... args) {
        GsonBuilder builder = FileUtils.applyModifiers(GSON_BUILDER, args);
        return (T)builder.create().fromJson(data, classObj);
    }

    public static <T> T getJsonData(File data, String encoding, Type typeObj, Modifiers ... args) throws Exception {
        return FileUtils.getJsonData(FileUtils.fileToString(data, encoding), typeObj, args);
    }

    public static <T> T getJsonData(File data, Type typeObj, Modifiers ... args) throws Exception {
        return FileUtils.getJsonData(data, "UTF-8", typeObj, args);
    }

    public static <T> T getJsonData(String data, Type typeObj, Modifiers ... args) {
        GsonBuilder builder = FileUtils.applyModifiers(GSON_BUILDER, args);
        return (T)builder.create().fromJson(data, typeObj);
    }

    public static <T> T getJsonData(T data, Class<T> classObj, Modifiers ... args) {
        return (T)FileUtils.getJsonData(data.toString(), classObj, args);
    }

    public static ThreadFactory getThreadFactory(String name) {
        return FileUtils.getOrCreateScheduler(name).getSecond();
    }

    public static Class<?> getValidClass(ClassLoader loader, boolean init, String ... paths) {
        return FileUtils.getValidClass(loader, init, false, paths);
    }

    public static InputStream getResourceAsStream(Class<?> fallbackClass, String pathToSearch) {
        InputStream in = null;
        boolean useFallback = false;
        try {
            in = CLASS_LOADER.getResourceAsStream(pathToSearch);
        }
        catch (Exception ex) {
            useFallback = true;
        }
        if (useFallback || in == null) {
            in = fallbackClass.getResourceAsStream(pathToSearch);
        }
        return in;
    }

    public static <T> T castOrConvert(Object obj, Class<T> targetClass) {
        if (targetClass.isAssignableFrom(obj.getClass())) {
            return targetClass.cast(obj);
        }
        if (obj instanceof String) {
            return FileUtils.convertStringToType((String)obj, targetClass);
        }
        Constants.LOG.debugError("Conversion or casting not supported between " + obj.getClass().getSimpleName() + " and " + targetClass.getSimpleName(), new Object[0]);
        return null;
    }

    private static <T> T convertStringToType(String value, Class<T> targetType) {
        Object obj;
        if (targetType.equals(Boolean.class) || targetType.equals(Boolean.TYPE)) {
            obj = Boolean.valueOf(value);
        } else if (targetType.equals(Byte.class) || targetType.equals(Byte.TYPE)) {
            obj = Byte.valueOf(value);
        } else if (targetType.equals(Short.class) || targetType.equals(Short.TYPE)) {
            obj = Short.valueOf(value);
        } else if (targetType.equals(Integer.class) || targetType.equals(Integer.TYPE)) {
            obj = Integer.valueOf(value);
        } else if (targetType.equals(Long.class) || targetType.equals(Long.TYPE)) {
            obj = Long.valueOf(value);
        } else if (targetType.equals(Float.class) || targetType.equals(Float.TYPE)) {
            obj = Float.valueOf(value);
        } else if (targetType.equals(Double.class) || targetType.equals(Double.TYPE)) {
            obj = Double.valueOf(value);
        } else {
            Constants.LOG.debugError("Conversion not supported for: " + targetType.getSimpleName(), new Object[0]);
            return null;
        }
        return (T)obj;
    }

    public static Class<?> loadClass(ClassLoader loader, String ... paths) {
        return FileUtils.getValidClass(loader, true, paths);
    }

    public static Class<?> loadClass(boolean useClassLoader, String ... paths) {
        return FileUtils.loadClass(useClassLoader ? CLASS_LOADER : null, paths);
    }

    public static Class<?> loadClass(String ... paths) {
        return FileUtils.loadClass(OSUtils.JAVA_SPEC < 16.0f, paths);
    }

    public static GsonBuilder applyModifiers(GsonBuilder instance, Modifiers ... args) {
        block4: for (Modifiers param : args) {
            switch (param.ordinal()) {
                case 0: {
                    instance.disableHtmlEscaping();
                    continue block4;
                }
                case 1: {
                    instance.setPrettyPrinting();
                    continue block4;
                }
            }
        }
        return instance;
    }

    public static enum Modifiers {
        DISABLE_ESCAPES,
        PRETTY_PRINT;

    }
}

