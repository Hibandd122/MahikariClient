/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.utils;

import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import mahikariui.utils.StringUtils;

public class MappingUtils {
    private static Map<String, String> classMap = null;
    private static String filePath = "/mappings.srg";

    public static boolean areMappingsLoaded() {
        return classMap != null;
    }

    public static void setFilePath(String filePath) {
        MappingUtils.filePath = filePath;
    }

    public static Set<String> getUnmappedClassesMatching(String start, BiPredicate<String, String> matchCondition) {
        Set<String> matches = StringUtils.newHashSet();
        if (MappingUtils.areMappingsLoaded()) {
            for (Map.Entry<String, String> entry : classMap.entrySet()) {
                if (!matchCondition.test(entry.getValue(), start)) continue;
                matches.add(entry.getKey());
            }
        }
        return matches;
    }

    public static Set<String> getUnmappedClassesMatching(String start, boolean exact) {
        return MappingUtils.getUnmappedClassesMatching(start, exact ? String::equals : String::startsWith);
    }

    public static Set<String> getUnmappedClassesMatching(String start) {
        return MappingUtils.getUnmappedClassesMatching(start, false);
    }
}

