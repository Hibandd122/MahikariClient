import java.io.*;
import java.net.*;
import java.nio.file.*;
import javax.swing.*;
import java.util.*;
import java.util.regex.*;
import java.awt.GridLayout;
import java.nio.charset.StandardCharsets;

public class MahikariUpdater {
    private static final String GITHUB_REPO = "Hibandd122/MahikariClient";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases";
    private static final String FILE_NAME = "mahikari-client.jar";
    private static final String CONFIG_FILE = "updater-config.properties";

    static class Release {
        String name;
        String downloadUrl;
        
        Release(String name, String downloadUrl) {
            this.name = name;
            this.downloadUrl = downloadUrl;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Fetch releases
            List<Release> releases = fetchReleases();
            if (releases.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Không tìm thấy phiên bản nào cho " + GITHUB_REPO, "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File modsDir = getSavedModsDirectory();
            if (!modsDir.exists()) {
                modsDir.mkdirs();
            }

            while (true) {
                JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
                panel.add(new JLabel("Chọn phiên bản để cài đặt:"));
                
                JComboBox<Release> versionBox = new JComboBox<>(releases.toArray(new Release[0]));
                panel.add(versionBox);
                
                JLabel pathLabel = new JLabel("Thư mục đích: " + modsDir.getAbsolutePath());
                pathLabel.setFont(pathLabel.getFont().deriveFont(11f));
                panel.add(pathLabel);

                Object[] options = {"Cài đặt", "Đổi thư mục", "Hủy bỏ"};
                int response = JOptionPane.showOptionDialog(null,
                    panel,
                    "Trình cập nhật Mahikari Client",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]);
                    
                if (response == 2 || response == JOptionPane.CLOSED_OPTION) {
                    return;
                }

                if (response == 1) { // Change Folder
                    JFileChooser chooser = new JFileChooser();
                    chooser.setCurrentDirectory(modsDir.getParentFile() != null ? modsDir.getParentFile() : modsDir);
                    chooser.setDialogTitle("Chọn thư mục cài đặt mod (.minecraft/mods)");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setAcceptAllFileFilterUsed(false);
                    
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        modsDir = chooser.getSelectedFile();
                        saveModsDirectory(modsDir);
                    }
                    continue; // Re-open dialog with new path
                }

                // Install selected version
                Release selected = (Release) versionBox.getSelectedItem();
                if (selected != null) {
                    File targetFile = new File(modsDir, FILE_NAME);
                    downloadFile(selected.downloadUrl, targetFile);
                    JOptionPane.showMessageDialog(null, "Đã cài đặt thành công " + selected.name + " tại:\n" + targetFile.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Cập nhật thất bại: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static List<Release> fetchReleases() throws Exception {
        List<Release> releases = new ArrayList<>();
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "MahikariUpdater/1.0");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("GitHub API trả về mã lỗi HTTP " + conn.getResponseCode());
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        String json = response.toString();
        
        // Split by release block: in GitHub API, each release starts with a distinct pattern like {"url": or "id":
        String[] blocks = json.split("\\{\\s*\"url\"\\s*:");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            Matcher nameMatcher = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(block);
            if (nameMatcher.find()) {
                String name = nameMatcher.group(1);
                Matcher urlMatcher = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"").matcher(block);
                String downloadUrl = null;
                if (urlMatcher.find()) {
                    downloadUrl = urlMatcher.group(1);
                } else {
                    downloadUrl = "https://github.com/" + GITHUB_REPO + "/releases/download/" + name + "/mahikari-client.jar";
                }
                if (releases.isEmpty()) {
                    name += " (Mới nhất)";
                }
                releases.add(new Release(name, downloadUrl));
            }
        }
        
        return releases;
    }

    private static void downloadFile(String urlStr, File targetFile) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "MahikariUpdater/1.0");
        
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestProperty("User-Agent", "MahikariUpdater/1.0");
        }
        
        if (conn.getResponseCode() != 200) {
            throw new IOException("Tải file thất bại: HTTP " + conn.getResponseCode());
        }
        
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private static File getModsDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        File mcDir;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            mcDir = new File(appData != null ? appData : userHome, ".minecraft");
        } else if (os.contains("mac")) {
            mcDir = new File(userHome, "Library/Application Support/minecraft");
        } else {
            mcDir = new File(userHome, ".minecraft");
        }

        return new File(mcDir, "mods");
    }

    private static File getSavedModsDirectory() {
        File defaultDir = getModsDirectory();
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
                String savedPath = props.getProperty("modsFolder");
                if (savedPath != null && !savedPath.trim().isEmpty()) {
                    File savedDir = new File(savedPath);
                    if (savedDir.exists() || savedDir.mkdirs()) {
                        return savedDir;
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return defaultDir;
    }

    private static void saveModsDirectory(File modsDir) {
        Properties props = new Properties();
        props.setProperty("modsFolder", modsDir.getAbsolutePath());
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Mahikari Updater Configuration");
        } catch (Exception e) {
            // Ignore
        }
    }
}
