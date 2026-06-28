package mahikariui.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import mahikariui.core.Constants;
import mahikariui.core.background.BackgroundBuilder;
import mahikariui.core.config.ConfigData;

public class Config {
    private static final String CONFIG_FILE_NAME = "mahikariui.json";
    private static Config instance;
    private static final Gson gson;
    private ConfigData data = new ConfigData();

    private Config() {
        this.loadConfig();
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
            instance.load();
        }
        return instance;
    }

    private void loadConfig() {
        File configFile;
        File configDir = new File(Constants.configDir);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        if (!(configFile = new File(configDir, CONFIG_FILE_NAME)).exists()) {
            this.createDefaultConfig(configFile);
        }
        try (FileReader reader = new FileReader(configFile);){
            ConfigData loaded = (ConfigData)gson.fromJson((Reader)reader, ConfigData.class);
            if (loaded != null) {
                this.data = loaded;
            }
        }
        catch (Throwable e) {
            Constants.LOG.printStackTrace(e, true, "MahikariUI Error:", "Config", System.out);
        }
    }

    private void createDefaultConfig(File configFile) {
        try {
            this.saveToFile(configFile);
        }
        catch (Throwable e) {
            Constants.LOG.printStackTrace(e, true, "MahikariUI Error:", "Config", System.out);
        }
    }

    public void load() {
        this.loadConfig();
        this.save();
    }

    public void save() {
        File configDir = new File(Constants.configDir);
        File configFile = new File(configDir, CONFIG_FILE_NAME);
        try {
            this.saveToFile(configFile);
        }
        catch (Throwable e) {
            Constants.LOG.printStackTrace(e, true, "MahikariUI Error:", "Config", System.out);
        }
    }

    private void saveToFile(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file);){
            gson.toJson((Object)this.data, (Appendable)writer);
        }
    }

    public int getBackgroundFrame() {
        return this.data.backgroundFrame;
    }

    public void setBackgroundFrame(int frame) {
        this.data.backgroundFrame = frame;
        this.save();
    }

    public String getBackground() {
        return this.data.background;
    }

    public void setBackground(String bg) {
        this.data.background = bg;
        this.save();
        BackgroundBuilder.selectBackground(bg);
    }

    public boolean hasAgreed() {
        return this.data.agreedToTerms;
    }

    public void setAgreed() {
        this.data.agreedToTerms = true;
        this.save();
    }

    public boolean isLowQualityMode() {
        return this.data.lowQualityMode;
    }

    public void setLowQualityMode(boolean mode) {
        this.data.lowQualityMode = mode;
        this.save();
    }

    public void exportConfig(File file) throws IOException {
        JsonObject export = new JsonObject();
        export.add("config", gson.toJsonTree((Object)this.data));
        export.addProperty("version", "1.0");
        export.addProperty("exportDate", (Number)System.currentTimeMillis());
        try (FileWriter writer = new FileWriter(file);){
            gson.toJson((JsonElement)export, (Appendable)writer);
        }
    }

    public void importConfig(File file) throws IOException {
        try (FileReader reader = new FileReader(file);){
            ConfigData imported_config;
            JsonObject imported = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            if (imported.has("config") && (imported_config = (ConfigData)gson.fromJson(imported.get("config"), ConfigData.class)) != null) {
                this.data = imported_config;
                this.save();
            }
        }
    }

    static {
        gson = new GsonBuilder().setPrettyPrinting().create();
    }
}
