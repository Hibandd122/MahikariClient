/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import mahikariui.core.Constants;
import mahikariui.utils.FileUtils;
import mahikariui.utils.StringUtils;

public class TranslationUtils {
    private String defaultLanguageId = "en_us";
    private boolean usingJson = false;
    private String modId;
    private String encoding;
    private boolean needsSync;
    private boolean usingAssetsPath = true;
    private boolean stripColors = false;
    private boolean stripFormatting = false;
    private String languageId = this.defaultLanguageId;
    private Consumer<Map<String, String>> onLanguageSync = null;
    private Function<String, String> languageSupplier = fallback -> fallback;
    private final Map<String, Map<String, String>> requestMap = StringUtils.newHashMap();
    private static final Pattern JSON_PATTERN = Pattern.compile("(?s)\\\\(.)");
    private final BiFunction<TranslationUtils, String, List<InputStream>> resourceSupplier = (instance, langPath) -> StringUtils.newArrayList();

    public TranslationUtils() {
        this(false);
    }

    public TranslationUtils(boolean useJson) {
        this("", useJson);
    }

    public TranslationUtils(String modId, boolean useJson) {
        this(modId, useJson, "UTF-8");
    }

    public TranslationUtils(String modId, boolean useJson, String encoding) {
        this.setUsingJson(useJson);
        this.setModId(modId);
        this.setEncoding(encoding);
    }

    public TranslationUtils build() {
        this.syncTranslations(this.getDefaultLanguage());
        this.needsSync = true;
        return this;
    }

    public void onTick() {
        boolean hasLanguageChanged;
        String currentLanguageId = this.getCurrentLanguage();
        boolean bl = hasLanguageChanged = !this.languageId.equals(currentLanguageId) && (!this.hasTranslationsFrom(currentLanguageId) || !this.requestMap.get(currentLanguageId).isEmpty());
        if (this.needsSync) {
            List<String> requestedKeys = StringUtils.newArrayList(this.requestMap.keySet());
            for (String key : requestedKeys) {
                this.syncTranslations(key, false);
            }
            this.needsSync = false;
        } else if (hasLanguageChanged) {
            this.syncTranslations(currentLanguageId);
        }
    }

    public void setUsingJson(boolean usingJson) {
        this.usingJson = usingJson;
    }

    public void setModId(String modId) {
        this.modId = StringUtils.getOrDefault(modId);
    }

    public void setEncoding(String encoding) {
        this.encoding = StringUtils.getOrDefault(encoding, "UTF-8");
    }

    public void setLanguage(String languageId) {
        String result = StringUtils.getOrDefault(languageId, this.defaultLanguageId);
        result = result != null ? result.toLowerCase() : "en_us";
        this.languageId = this.usingJson ? result.toLowerCase() : result;
    }

    public TranslationUtils setUsingAssetsPath(boolean usingAssetsPath) {
        this.usingAssetsPath = usingAssetsPath;
        return this;
    }

    public TranslationUtils setStripColors(boolean stripColors) {
        this.stripColors = stripColors;
        return this;
    }

    public TranslationUtils setStripFormatting(boolean stripFormatting) {
        this.stripFormatting = stripFormatting;
        return this;
    }

    public String getDefaultLanguage() {
        return this.usingJson ? (this.defaultLanguageId != null ? this.defaultLanguageId.toLowerCase() : "en_us") : "en_us";
    }

    public String getModId() {
        return this.modId;
    }

    private String getCurrentLanguage() {
        String result = this.languageSupplier.apply(this.defaultLanguageId);
        return this.usingJson ? result.toLowerCase() : result;
    }

    public TranslationUtils setDefaultLanguage(String languageId) {
        this.defaultLanguageId = languageId;
        return this;
    }

    public boolean hasTranslationsFrom(String languageId) {
        return this.requestMap.containsKey(languageId);
    }

    public boolean hasTranslationFrom(String languageId, String translationKey) {
        if (this.hasTranslationsFrom(languageId)) {
            return this.requestMap.get(languageId).containsKey(translationKey);
        }
        return this.getTranslationMapFrom(languageId).containsKey(translationKey);
    }

    public String getAssetsPath() {
        return this.usingAssetsPath ? String.format("/assets/%s/", this.getModId()) : "/";
    }

    private List<InputStream> getLocaleStreamsFrom(String languageId, String ext) {
        String assetsPath = this.getAssetsPath();
        String langPath = String.format("lang/%s.%s", languageId, ext);
        List<InputStream> results = StringUtils.newArrayList();
        InputStream local = FileUtils.getResourceAsStream(TranslationUtils.class, assetsPath + langPath);
        if (local != null) {
            results.add(local);
        }
        results.addAll((Collection)this.resourceSupplier.apply(this, langPath));
        return results;
    }

    private List<InputStream> getLocaleStreamsFrom(String languageId) {
        return this.getLocaleStreamsFrom(languageId, this.usingJson ? "json" : "lang");
    }

    private Map<String, String> getTranslationMapFrom(String languageId, String encoding, List<InputStream> data) {
        boolean hasError = false;
        boolean hadBefore = this.hasTranslationsFrom(languageId);
        this.requestMap.remove(languageId);
        Map<String, String> translationMap = StringUtils.newHashMap();
        if (data != null && !data.isEmpty()) {
            for (InputStream in : data) {
                if (in != null) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName(encoding)));
                        try {
                            String currentString;
                            while ((currentString = reader.readLine()) != null) {
                                String[] splitTranslation;
                                if ((currentString = currentString.trim()).startsWith("#") || currentString.startsWith("[{}]") || !(this.usingJson ? currentString.contains(":") : currentString.contains("="))) continue;
                                String[] stringArray = splitTranslation = this.usingJson ? currentString.split(":", 2) : currentString.split("=", 2);
                                if (this.usingJson) {
                                    String str1 = splitTranslation[0].substring(1, splitTranslation[0].length() - 1).trim();
                                    String str2 = splitTranslation[1].substring(2, splitTranslation[1].length() - (splitTranslation[1].endsWith(",") ? 2 : 1)).trim();
                                    translationMap.put(StringUtils.replaceMatches(JSON_PATTERN, str1, "$1"), StringUtils.replaceMatches(JSON_PATTERN, str2, "$1"));
                                    continue;
                                }
                                translationMap.put(splitTranslation[0].trim(), splitTranslation[1].trim());
                            }
                            in.close();
                            continue;
                        }
                        finally {
                            reader.close();
                            continue;
                        }
                    }
                    catch (Exception ex) {
                        Constants.LOG.error("An exception has occurred while loading Translation Mappings, aborting scan to prevent issues...", new Object[0]);
                        Constants.LOG.debugError(ex);
                        hasError = true;
                    }
                } else {
                    hasError = true;
                }
                break;
            }
        } else {
            hasError = true;
        }
        if (hasError) {
            Constants.LOG.error("Translations for " + this.getModId() + " do not exist for " + languageId, new Object[0]);
            translationMap.clear();
            this.requestMap.put(languageId, translationMap);
            this.setLanguage(this.defaultLanguageId);
        } else {
            Constants.LOG.debugInfo((hadBefore ? "Refreshed" : "Added") + " translations for " + this.getModId() + " for " + languageId, new Object[0]);
            this.requestMap.put(languageId, translationMap);
        }
        return translationMap;
    }

    private Map<String, String> getTranslationMapFrom(String languageId, String encoding) {
        return this.getTranslationMapFrom(languageId, encoding, this.getLocaleStreamsFrom(languageId));
    }

    private Map<String, String> getTranslationMapFrom(String languageId) {
        return this.getTranslationMapFrom(languageId, "UTF-8");
    }

    public void syncTranslations(String languageId, boolean setLanguage) {
        if (setLanguage) {
            this.setLanguage(languageId);
        }
        Map<String, String> results = this.getTranslationMapFrom(languageId, this.encoding);
        if (this.onLanguageSync != null) {
            this.onLanguageSync.accept(results);
        }
    }

    public TranslationUtils setOnLanguageSync(Consumer<Map<String, String>> onLanguageSync) {
        this.onLanguageSync = onLanguageSync;
        return this;
    }

    public void syncTranslations(String languageId) {
        this.syncTranslations(languageId, true);
    }

    public void syncTranslations() {
        this.needsSync = true;
    }

    public String getTranslationFrom(String languageId, String translationKey) {
        if (this.hasTranslationFrom(languageId, translationKey)) {
            return this.requestMap.get(languageId).get(translationKey);
        }
        return null;
    }

    public String translateFrom(String languageId, boolean stripColors, boolean stripFormatting, String translationKey, Object ... parameters) {
        boolean hasError = false;
        String translatedString = translationKey;
        try {
            if (this.hasTranslationFrom(languageId, translationKey)) {
                String rawString = this.getTranslationFrom(languageId, translationKey);
                translatedString = parameters.length > 0 ? String.format(rawString, parameters) : rawString;
            } else {
                hasError = true;
            }
        }
        catch (Exception ex) {
            Constants.LOG.error("Exception parsing " + translationKey + " from " + languageId, new Object[0]);
            Constants.LOG.debugError(ex);
            return translationKey;
        }
        if (hasError) {
            Constants.LOG.debugError("Unable to retrieve a translation for " + translationKey + " from " + languageId, new Object[0]);
            if (!languageId.equals(this.getDefaultLanguage())) {
                Constants.LOG.debugError("Attempting to retrieve default translation for " + translationKey, new Object[0]);
                return this.translateFrom(this.getDefaultLanguage(), stripColors, stripFormatting, translationKey, parameters);
            }
        }
        String result = translatedString;
        if (stripFormatting && stripColors) {
            result = StringUtils.stripAllFormatting(result);
        } else {
            if (stripColors) {
                result = StringUtils.stripColors(result);
            }
            if (stripFormatting) {
                result = StringUtils.stripFormatting(result);
            }
        }
        return result;
    }

    public String translateFrom(boolean stripColors, boolean stripFormatting, String translationKey, Object ... parameters) {
        return this.translateFrom(this.getDefaultLanguage(), stripColors, stripFormatting, translationKey, parameters);
    }

    public String translateFrom(String languageId, String translationKey, Object ... parameters) {
        return this.translateFrom(languageId, this.stripColors, this.stripFormatting, translationKey, parameters);
    }

    public String translateFrom(String translationKey, Object ... parameters) {
        return this.translateFrom(this.getDefaultLanguage(), translationKey, parameters);
    }

    public String translate(String translationKey, Object ... parameters) {
        return this.translateFrom(this.languageId, translationKey, parameters);
    }
}

