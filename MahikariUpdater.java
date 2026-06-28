import java.io.*;
import java.net.*;
import java.nio.file.*;
import javax.swing.*;
import java.util.*;
import java.util.regex.*;
import java.awt.GridLayout;

public class MahikariUpdater {
    private static final String GITHUB_REPO = "Hibandd122/MahikariClient";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases";
    private static final String FILE_NAME = "mahikari-client.jar";

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
                JOptionPane.showMessageDialog(null, "No releases found for " + GITHUB_REPO, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File modsDir = getModsDirectory();
            if (!modsDir.exists()) {
                modsDir.mkdirs();
            }

            while (true) {
                JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
                panel.add(new JLabel("Select version to install:"));
                
                JComboBox<Release> versionBox = new JComboBox<>(releases.toArray(new Release[0]));
                panel.add(versionBox);
                
                JLabel pathLabel = new JLabel("Target: " + modsDir.getAbsolutePath());
                pathLabel.setFont(pathLabel.getFont().deriveFont(11f));
                panel.add(pathLabel);

                Object[] options = {"Install", "Change Folder", "Cancel"};
                int response = JOptionPane.showOptionDialog(null,
                    panel,
                    "Mahikari Updater",
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
                    chooser.setDialogTitle("Select Installation Folder");
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setAcceptAllFileFilterUsed(false);
                    
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        modsDir = chooser.getSelectedFile();
                    }
                    continue; // Re-open dialog with new path
                }

                // Install selected version
                Release selected = (Release) versionBox.getSelectedItem();
                if (selected != null) {
                    File targetFile = new File(modsDir, FILE_NAME);
                    downloadFile(selected.downloadUrl, targetFile);
                    JOptionPane.showMessageDialog(null, "Installed " + selected.name + " successfully to:\n" + targetFile.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Update failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            throw new Exception("GitHub API returned " + conn.getResponseCode());
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        String json = response.toString();
        
        // Simple regex parser for JSON to avoid dependencies
        Matcher nameMatcher = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        Matcher urlMatcher = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"").matcher(json);
        
        while (nameMatcher.find() && urlMatcher.find()) {
            String name = nameMatcher.group(1);
            String downloadUrl = urlMatcher.group(1);
            if (releases.isEmpty()) {
                name += " (Latest)";
            }
            releases.add(new Release(name, downloadUrl));
        }
        
        // Fallback if no specific jar found (can happen if release has no assets)
        if (releases.isEmpty()) {
            nameMatcher.reset();
            while (nameMatcher.find()) {
                String name = nameMatcher.group(1);
                String fallbackUrl = "https://github.com/" + GITHUB_REPO + "/releases/download/" + name + "/mahikari-client.jar";
                releases.add(new Release(name, fallbackUrl));
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
            throw new IOException("Failed to download file: HTTP " + conn.getResponseCode());
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
}
