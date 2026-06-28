/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.lenni0451.reflect.stream.RStream
 *  net.lenni0451.reflect.stream.field.FieldStream
 *  net.lenni0451.reflect.stream.field.FieldWrapper
 *  net.lenni0451.reflect.stream.method.MethodStream
 *  net.lenni0451.reflect.stream.method.MethodWrapper
 */
package mahikariui.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;





import mahikariui.impl.Pair;
import mahikariui.utils.MathUtils;

public class StringUtils {
    public static final char COLOR_CHAR = '\u00a7';
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
    public static final Pattern NEW_LINE_PATTERN = Pattern.compile("(\\r\\n|\\r|\\n|\\\\n)");
    public static final Pattern STRIP_ALL_FORMATTING_PATTERN = Pattern.compile("(?i)\u00a7[0-9A-FK-OR]");
    public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\u00a7[0-9A-F]");
    public static final Pattern STRIP_FORMATTING_PATTERN = Pattern.compile("(?i)\u00a7[K-O]");
    public static final String TAB_SPACE = "    ";
    public static final Predicate<String> NULL_OR_EMPTY = StringUtils::isNullOrEmpty;
    private static final Pattern COLOR_PATTERN = Pattern.compile("^(?:0x([\\dA-Fa-f]{1,8})|#?([\\dA-Fa-f]{6}([\\dA-Fa-f]{2})?))$");
    private static final Pattern CURLY_BRACES_PATTERN = Pattern.compile("[{}]");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");
    private static final Pattern TRIMMED_UUID_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
    private static final Pattern FULL_UUID_PATTERN = Pattern.compile("(\\w{8})-(\\w{4})-(\\w{4})-(\\w{4})-(\\w{12})");
    private static final Pattern BRACKET_PATTERN = Pattern.compile("\\([^0-9]*\\d+[^0-9]*\\)");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public static boolean isNullOrEmpty(String entry, boolean allowWhitespace) {
        if (entry != null) {
            entry = allowWhitespace ? entry : entry.trim();
        }
        return entry == null || entry.isEmpty() || entry.equalsIgnoreCase("null");
    }

    public static boolean isNullOrEmpty(String entry) {
        return StringUtils.isNullOrEmpty(entry, false);
    }

    public static Pair<Boolean, Boolean> getValidBoolean(String entry) {
        Pair<Boolean, Boolean> finalSet = new Pair<Boolean, Boolean>();
        if (!StringUtils.isNullOrEmpty(entry)) {
            try {
                finalSet.setSecond(Boolean.parseBoolean(entry));
                finalSet.setFirst(true);
            }
            catch (Exception ex) {
                finalSet.setFirst(false);
            }
        } else {
            finalSet.setFirst(false);
        }
        return finalSet;
    }

    public static String getStackTrace(Throwable ex) {
        if (ex == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    public static String replaceMatches(Pattern pattern, String input, String replacement) {
        return StringUtils.isNullOrEmpty(input) ? input : pattern.matcher(input).replaceAll(replacement);
    }

    @SafeVarargs
    public static <T> List<T> newArrayList(T ... elements) {
        List<T> data = StringUtils.newArrayList();
        Collections.addAll(data, elements);
        return data;
    }

    public static <T> List<T> newArrayList() {
        return new ArrayList();
    }

    public static <T> List<T> newArrayList(Iterator<T> iterator) {
        List<T> list = StringUtils.newArrayList();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    public static <T> List<T> newArrayList(Iterable<T> iterable) {
        return StringUtils.newArrayList(iterable.iterator());
    }

    public static <T> LinkedList<T> newLinkedList() {
        return new LinkedList();
    }

    public static <K, V> Map<K, V> newConcurrentHashMap() {
        return new ConcurrentHashMap();
    }

    public static <K, V> Map<K, V> newConcurrentHashMap(Map<? extends K, ? extends V> map) {
        return new ConcurrentHashMap<K, V>(map);
    }

    public static <K extends Comparable<? super K>, V> ConcurrentSkipListMap<K, V> newConcurrentMap() {
        return new ConcurrentSkipListMap();
    }

    public static <K, V> ConcurrentSkipListMap<K, V> newConcurrentMap(Comparator<? super K> comparator) {
        return new ConcurrentSkipListMap(comparator);
    }

    public static <K extends Comparable<? super K>, V> ConcurrentSkipListMap<K, V> newConcurrentMap(Map<? extends K, ? extends V> map) {
        return new ConcurrentSkipListMap<K, V>(map);
    }

    public static <K extends Comparable<? super K>, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap();
    }

    public static <K, V> TreeMap<K, V> newTreeMap(Comparator<? super K> comparator) {
        return new TreeMap(comparator);
    }

    public static <K extends Comparable<? super K>, V> TreeMap<K, V> newTreeMap(Map<? extends K, ? extends V> map) {
        return new TreeMap<K, V>(map);
    }

    public static <T> boolean elementExists(T[] data, int index) {
        return StringUtils.elementExists(Arrays.asList(data), index);
    }

    public static <T> boolean elementExists(List<T> data, int index) {
        boolean result;
        try {
            result = data.size() >= index && data.get(index) != null;
        }
        catch (Exception ex) {
            result = false;
        }
        return result;
    }

    public static String stripColors(String input) {
        return StringUtils.stripMatches(STRIP_COLOR_PATTERN, input);
    }

    public static List<String> splitTextByNewLine(String original, boolean allowWhitespace) {
        if (!StringUtils.isNullOrEmpty(original, allowWhitespace)) {
            return StringUtils.newArrayList(NEW_LINE_PATTERN.split(original));
        }
        return StringUtils.newArrayList();
    }

    public static <T> List<T> addEntriesNotPresent(List<T> original, List<T> newList) {
        for (T entry : newList) {
            if (original.contains(entry)) continue;
            original.add(entry);
        }
        return original;
    }

    public static String capitalizeWord(String str, int timesToCheck) {
        StringBuilder s = new StringBuilder();
        int charIndex = 32;
        int timesLeft = timesToCheck;
        for (int index = 0; index < str.length(); ++index) {
            if (charIndex == 32 && str.charAt(index) != ' ' && (timesLeft > 0 || timesLeft == -1)) {
                s.append(Character.toUpperCase(str.charAt(index)));
                if (timesLeft > 0) {
                    --timesLeft;
                }
            } else {
                s.append(str.charAt(index));
            }
            charIndex = str.charAt(index);
        }
        return s.toString().trim();
    }

    public static String capitalizeWord(String str) {
        return StringUtils.capitalizeWord(str, -1);
    }



    @SafeVarargs
    public static <T> Set<T> newHashSet(T ... elements) {
        Set<T> data = StringUtils.newHashSet();
        Collections.addAll(data, elements);
        return data;
    }

    public static <T> Set<T> newHashSet() {
        return new HashSet();
    }

    public static <T> Set<T> newHashSet(Iterator<T> iterator) {
        Set<T> set = StringUtils.newHashSet();
        while (iterator.hasNext()) {
            set.add(iterator.next());
        }
        return set;
    }

    public static Pair<Boolean, Integer> getValidInteger(Object entry) {
        return entry != null ? StringUtils.getValidInteger(entry.toString()) : new Pair<Boolean, Integer>(false, 0);
    }

    public static Pair<Boolean, Long> getValidLong(Object entry) {
        return entry != null ? StringUtils.getValidLong(entry.toString()) : new Pair<Boolean, Long>(false, 0L);
    }

    public static Pair<Boolean, Long> getValidLong(String entry) {
        Pair<Boolean, Long> finalSet = new Pair<Boolean, Long>();
        if (!StringUtils.isNullOrEmpty(entry)) {
            try {
                finalSet.setSecond(Long.parseLong(entry));
                finalSet.setFirst(true);
            }
            catch (Exception ex) {
                finalSet.setFirst(false);
            }
        } else {
            finalSet.setFirst(false);
        }
        return finalSet;
    }

    public static Pair<Boolean, Integer> getValidInteger(String entry) {
        Pair<Boolean, Integer> finalSet = new Pair<Boolean, Integer>();
        if (!StringUtils.isNullOrEmpty(entry)) {
            try {
                finalSet.setSecond(Integer.parseInt(entry));
                finalSet.setFirst(true);
            }
            catch (Exception ex) {
                finalSet.setFirst(false);
            }
        } else {
            finalSet.setFirst(false);
        }
        return finalSet;
    }

    public static byte[] getBytes(String original, String encoding) {
        try {
            if (!StringUtils.isNullOrEmpty(encoding)) {
                return original.getBytes(encoding);
            }
            return StringUtils.getBytes(original, DEFAULT_CHARSET.name());
        }
        catch (Exception ex) {
            return StringUtils.getBytes(original, DEFAULT_CHARSET.name());
        }
    }

    public static byte[] getBytes(String original) {
        return StringUtils.getBytes(original, null);
    }

    public static <T> Set<T> newHashSet(Iterable<T> iterable) {
        return StringUtils.newHashSet(iterable.iterator());
    }

    public static <T> List<T> addEntriesNotPresent(List<T> original, Predicate<? super T> filter, List<T> newList) {
        newList = newList.stream().filter(filter).collect(Collectors.toList());
        return StringUtils.addEntriesNotPresent(original, newList);
    }

    public static <T> List<T> addEntriesNotPresent(List<T> original, Set<T> newList) {
        return StringUtils.addEntriesNotPresent(original, StringUtils.newArrayList(newList));
    }

    public static <T> List<T> addEntriesNotPresent(List<T> original, Predicate<? super T> filter, Set<T> newList) {
        newList = newList.stream().filter(filter).collect(Collectors.toSet());
        return StringUtils.addEntriesNotPresent(original, newList);
    }

    public static <T> List<T> addEntriesNotPresent(List<T> original, T[] newList) {
        return StringUtils.addEntriesNotPresent(original, Arrays.asList(newList));
    }

    public static <K, V> Map<K, V> newHashMap() {
        return new HashMap();
    }

    public static <K, V> Map<K, V> newHashMap(Map<? extends K, ? extends V> map) {
        return new HashMap<K, V>(map);
    }

    public static List<String> splitTextByNewLine(String original) {
        return StringUtils.splitTextByNewLine(original, false);
    }

    public static String stripMatches(Pattern pattern, String input) {
        return StringUtils.replaceMatches(pattern, input, "");
    }

    public static String stripFormatting(String input) {
        return StringUtils.stripMatches(STRIP_FORMATTING_PATTERN, input);
    }

    public static String stripAllFormatting(String input) {
        return StringUtils.stripMatches(STRIP_ALL_FORMATTING_PATTERN, input);
    }

    public static String normalizeWhitespace(String input) {
        return StringUtils.replaceMatches(WHITESPACE_PATTERN, input, " ");
    }

    public static String convertString(String original, String encoding, boolean decode) {
        try {
            if (decode) {
                return new String(StringUtils.getBytes(original), encoding);
            }
            byte[] bytes = StringUtils.getBytes(original, encoding);
            return new String(bytes, 0, bytes.length, DEFAULT_CHARSET);
        }
        catch (Exception ex) {
            return original;
        }
    }

    public static Object[] getDynamicArray(Object original) {
        if (!(original instanceof Object[])) {
            try {
                int len = Array.getLength(original);
                Object[] objects = new Object[len];
                for (int i = 0; i < len; ++i) {
                    objects[i] = Array.get(original, i);
                }
                return objects;
            }
            catch (Throwable ex) {
                return null;
            }
        }
        return (Object[])original;
    }

    public static String minifyString(String source, int length) {
        if (!StringUtils.isNullOrEmpty(source)) {
            return MathUtils.isWithinValue(length, 0.0, source.length(), true, true) ? source.substring(0, length) : source;
        }
        return "";
    }

    public static String formatIdentifier(String originalId, boolean formatToId) {
        return StringUtils.formatIdentifier(originalId, formatToId, false);
    }

    public static String formatIdentifier(String originalId, boolean formatToId, boolean avoid) {
        if (StringUtils.isNullOrEmpty(originalId)) {
            return originalId;
        }
        String formattedKey = originalId;
        if (formattedKey.equals("WorldProvider")) {
            formattedKey = "overworld";
        } else {
            formattedKey = formattedKey.replace("WorldProvider", "").replace("BiomeGen", "").replace("MobSpawner", "");
            formattedKey = StringUtils.normalizeWhitespace(formattedKey);
            if ((formattedKey = StringUtils.stripMatches(CURLY_BRACES_PATTERN, formattedKey)).contains(":")) {
                formattedKey = formattedKey.split(":", 2)[1];
            }
        }
        formattedKey = switch (formattedKey.toLowerCase()) {
            case "surface" -> "overworld";
            case "hell", "nether" -> "the_nether";
            case "end", "sky" -> "the_end";
            default -> formattedKey;
        };
        return formatToId ? StringUtils.formatAsIcon(formattedKey, "_") : StringUtils.formatWord(formattedKey, avoid);
    }

    public static String formatToCamel(String original) {
        if (StringUtils.isNullOrEmpty(original)) {
            return original;
        }
        String[] words = original.split("[\\W_]+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; ++i) {
            Object word = words[i];
            word = i == 0 ? (((String)word).isEmpty() ? word : ((String)word).toLowerCase()) : (((String)word).isEmpty() ? word : Character.toUpperCase(((String)word).charAt(0)) + ((String)word).substring(1).toLowerCase());
            builder.append((String)word);
        }
        return builder.toString();
    }

    public static String formatAddress(String input, boolean returnPort) {
        if (!StringUtils.isNullOrEmpty(input)) {
            String[] formatted = input.split(":", 2);
            return !returnPort ? (StringUtils.elementExists(formatted, 0) ? formatted[0].trim() : "127.0.0.1") : (StringUtils.elementExists(formatted, 1) ? formatted[1].trim() : "25565");
        }
        return !returnPort ? "127.0.0.1" : "25565";
    }



    public static String formatWord(String original) {
        return StringUtils.formatWord(original, false);
    }

    public static String formatWord(String original, boolean avoid) {
        return StringUtils.formatWord(original, avoid, false);
    }

    public static String formatWord(String original, boolean avoid, boolean skipSymbolReplacement) {
        return StringUtils.formatWord(original, avoid, skipSymbolReplacement, -1);
    }

    public static String formatWord(String original, boolean avoid, boolean skipSymbolReplacement, int caseCheckTimes) {
        if (StringUtils.isNullOrEmpty(original) || avoid) {
            return original;
        }
        String formattedKey = original.trim();
        formattedKey = StringUtils.normalizeWhitespace(formattedKey);
        if (!skipSymbolReplacement) {
            formattedKey = formattedKey.replace("_", " ").replace("-", " ");
            formattedKey = StringUtils.stripMatches(BRACKET_PATTERN, formattedKey);
            formattedKey = StringUtils.stripMatches(STRIP_ALL_FORMATTING_PATTERN, formattedKey);
        }
        return StringUtils.removeRepeatWords(StringUtils.capitalizeWord(formattedKey, caseCheckTimes)).trim();
    }

    public static String formatAsIcon(String original, String whitespaceIndex) {
        if (StringUtils.isNullOrEmpty(original)) {
            return original;
        }
        String formattedKey = original.trim();
        formattedKey = StringUtils.replaceMatches(WHITESPACE_PATTERN, formattedKey, whitespaceIndex);
        formattedKey = StringUtils.replaceMatches(NON_ALPHANUMERIC_PATTERN, formattedKey, "_").toLowerCase();
        return formattedKey;
    }

    public static String formatAsIcon(String original) {
        return StringUtils.formatAsIcon(original, "");
    }

    public static Pair<Boolean, Matcher> isValidColor(String entry) {
        Matcher m = COLOR_PATTERN.matcher(entry);
        return new Pair<Boolean, Matcher>(m.find(), m);
    }

    public static boolean isValidColorCode(String entry) {
        return !StringUtils.isNullOrEmpty(entry) && (StringUtils.isValidColor(entry).getFirst() != false || StringUtils.getValidInteger(entry).getFirst() != false);
    }

    public static <T> T getOrDefault(T primary, T secondary, Predicate<T> condition) {
        return condition.test(primary) ? primary : secondary;
    }

    public static <T> T getOrDefault(T primary, T secondary) {
        return (T)StringUtils.getOrDefault(primary, secondary, Objects::nonNull);
    }

    public static <T> T getOrDefault(T primary) {
        return StringUtils.getOrDefault(primary, null);
    }

    public static String getOrDefault(String primary, String secondary) {
        return StringUtils.getOrDefault(primary, secondary, NULL_OR_EMPTY.negate());
    }

    public static String getOrDefault(String primary) {
        return StringUtils.getOrDefault(primary, "");
    }

    public static boolean isValidUuid(String input) {
        return !StringUtils.isNullOrEmpty(input) && (input.contains("-") ? FULL_UUID_PATTERN : TRIMMED_UUID_PATTERN).matcher(input).find();
    }

    public static String removeRepeatWords(String original) {
        String[] wordList;
        if (StringUtils.isNullOrEmpty(original)) {
            return original;
        }
        String lastWord = "";
        StringBuilder finalString = new StringBuilder();
        for (String word : wordList = original.split(" ")) {
            if (!StringUtils.isNullOrEmpty(lastWord) && word.equalsIgnoreCase(lastWord)) continue;
            finalString.append(word).append(" ");
            lastWord = word;
        }
        return finalString.toString().trim();
    }

    public static <K, V> Map<K, V> newLinkedHashMap() {
        return new LinkedHashMap();
    }

    public static <K, V> Map<K, V> newLinkedHashMap(Map<? extends K, ? extends V> map) {
        return new LinkedHashMap<K, V>(map);
    }

    public static String normalizeLines(String input) {
        return StringUtils.replaceMatches(NEW_LINE_PATTERN, input, "\n");
    }

    public static String normalize(String input) {
        return StringUtils.stripAllFormatting(StringUtils.normalizeLines(input));
    }
}

