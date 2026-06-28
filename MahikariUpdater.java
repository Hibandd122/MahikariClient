import java.io.*;
import java.net.*;
import java.nio.file.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;

public class MahikariUpdater extends JFrame {
    private static final String GITHUB_REPO = "Hibandd122/MahikariClient";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases";
    private static final String FILE_NAME = "mahikari-client.jar";
    private static final String CONFIG_FILE = "updater-config.properties";

    // UI Colors
    private static final Color COLOR_BG = new Color(0x141820);
    private static final Color COLOR_CARD = new Color(0x1A1D2E);
    private static final Color COLOR_BORDER = new Color(0x2D3250);
    private static final Color COLOR_INPUT_BG = new Color(0x1E2530);
    private static final Color COLOR_ACCENT = new Color(0x00D9FF);
    private static final Color COLOR_ACCENT_HOVER = new Color(0x33E2FF);
    private static final Color COLOR_TEXT_PRIMARY = Color.WHITE;
    private static final Color COLOR_TEXT_MUTED = new Color(0xB0B8CC);

    private JTextField pathField;
    private JComboBox<Release> versionBox;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton installBtn;
    private JButton browseBtn;
    private JButton closeBtn;
    private File modsDir;

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

    public MahikariUpdater() {
        setTitle("Trình cập nhật Mahikari Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 360);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Custom Content Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(COLOR_BG);
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setContentPane(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        // Title Label
        JLabel titleLabel = new JLabel("MAHIKARI CLIENT UPDATER", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(COLOR_ACCENT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // Subtitle/Author Label
        JLabel subLabel = new JLabel("Hỗ trợ cài đặt & nâng cấp phiên bản", JLabel.CENTER);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(COLOR_TEXT_MUTED);
        gbc.gridy = 1;
        mainPanel.add(subLabel, gbc);

        // Separator line
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 15, 0);
        mainPanel.add(createSeparator(), gbc);
        gbc.insets = new Insets(6, 0, 6, 0);

        // Folder selection section
        JLabel pathTitle = new JLabel("Thư mục cài đặt mod (.minecraft/mods):");
        pathTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pathTitle.setForeground(COLOR_TEXT_PRIMARY);
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(pathTitle, gbc);

        JPanel pathPanel = new JPanel(new BorderLayout(8, 0));
        pathPanel.setOpaque(false);

        pathField = new JTextField();
        pathField.setBackground(COLOR_INPUT_BG);
        pathField.setForeground(COLOR_TEXT_PRIMARY);
        pathField.setCaretColor(COLOR_TEXT_PRIMARY);
        pathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        pathField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pathPanel.add(pathField, BorderLayout.CENTER);

        browseBtn = createModernButton("Chọn...", false);
        browseBtn.addActionListener(e -> chooseDirectory());
        pathPanel.add(browseBtn, BorderLayout.EAST);

        gbc.gridy = 4;
        mainPanel.add(pathPanel, gbc);

        // Version selection section
        JLabel versionTitle = new JLabel("Chọn phiên bản cài đặt (Hỗ trợ tải bản cũ):");
        versionTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        versionTitle.setForeground(COLOR_TEXT_PRIMARY);
        gbc.gridy = 5;
        mainPanel.add(versionTitle, gbc);

        versionBox = new JComboBox<>();
        versionBox.setBackground(COLOR_INPUT_BG);
        versionBox.setForeground(COLOR_TEXT_PRIMARY);
        versionBox.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        versionBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        // Customize ComboBox renderer for dark mode
        versionBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBackground(isSelected ? COLOR_BORDER : COLOR_INPUT_BG);
                label.setForeground(COLOR_TEXT_PRIMARY);
                label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                return label;
            }
        });
        gbc.gridy = 6;
        mainPanel.add(versionBox, gbc);

        // Progress Bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setBackground(COLOR_INPUT_BG);
        progressBar.setForeground(COLOR_ACCENT);
        progressBar.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        progressBar.setVisible(false);
        gbc.gridy = 7;
        mainPanel.add(progressBar, gbc);

        // Status label
        statusLabel = new JLabel("Đang tải danh sách phiên bản...");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(COLOR_TEXT_MUTED);
        gbc.gridy = 8;
        mainPanel.add(statusLabel, gbc);

        // Action Buttons Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        installBtn = createModernButton("CÀI ĐẶT", true);
        installBtn.setEnabled(false);
        installBtn.addActionListener(e -> startInstallation());
        actionPanel.add(installBtn);

        closeBtn = createModernButton("THOÁT", false);
        closeBtn.addActionListener(e -> System.exit(0));
        actionPanel.add(closeBtn);

        gbc.gridy = 9;
        gbc.insets = new Insets(12, 0, 0, 0);
        mainPanel.add(actionPanel, gbc);

        // Load configuration and data
        modsDir = getSavedModsDirectory();
        pathField.setText(modsDir.getAbsolutePath());
        
        // Fetch releases asynchronously
        new Thread(this::loadReleases).start();
    }

    private JComponent createSeparator() {
        return new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(COLOR_BORDER);
                g.drawLine(0, 0, getWidth(), 0);
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(getWidth(), 1);
            }
        };
    }

    private JButton createModernButton(String text, boolean highlight) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        if (highlight) {
            btn.setBackground(COLOR_ACCENT);
            btn.setForeground(COLOR_BG);
            btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        } else {
            btn.setBackground(COLOR_INPUT_BG);
            btn.setForeground(COLOR_TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 14, 5, 14)
            ));
        }

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(highlight ? COLOR_ACCENT_HOVER : COLOR_BORDER);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(highlight ? COLOR_ACCENT : COLOR_INPUT_BG);
                }
            }
        });
        return btn;
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        File current = new File(pathField.getText().trim());
        chooser.setCurrentDirectory(current.exists() ? current : new File("C:\\"));
        chooser.setDialogTitle("Chọn thư mục cài đặt mod");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            pathField.setText(selected.getAbsolutePath());
            modsDir = selected;
            saveModsDirectory(selected);
        }
    }

    private void loadReleases() {
        try {
            java.util.List<Release> releases = fetchReleasesList();
            SwingUtilities.invokeLater(() -> {
                if (releases.isEmpty()) {
                    statusLabel.setText("Lỗi: Không tìm thấy phiên bản phát hành nào trên GitHub.");
                    return;
                }
                for (Release r : releases) {
                    versionBox.addItem(r);
                }
                installBtn.setEnabled(true);
                statusLabel.setText("Đã sẵn sàng. Chọn phiên bản và nhấn 'Cài đặt'.");
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Lỗi kết nối GitHub: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Không thể tải danh sách phiên bản:\n" + e.getMessage(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private java.util.List<Release> fetchReleasesList() throws Exception {
        java.util.List<Release> releases = new ArrayList<>();
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "MahikariUpdater/2.0");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP " + conn.getResponseCode());
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        
        String json = response.toString();
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
                    downloadUrl = "https://github.com/" + GITHUB_REPO + "/releases/download/" + name + "/" + FILE_NAME;
                }
                if (releases.isEmpty()) {
                    name += " (Mới nhất)";
                }
                releases.add(new Release(name, downloadUrl));
            }
        }
        return releases;
    }

    private void startInstallation() {
        String pathText = pathField.getText().trim();
        if (pathText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập hoặc chọn thư mục lưu trữ mod!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        modsDir = new File(pathText);
        if (!modsDir.exists() && !modsDir.mkdirs()) {
            JOptionPane.showMessageDialog(this, "Không thể tạo thư mục cài đặt:\n" + modsDir.getAbsolutePath(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        saveModsDirectory(modsDir);

        Release selected = (Release) versionBox.getSelectedItem();
        if (selected == null) return;

        // UI state when downloading
        installBtn.setEnabled(false);
        browseBtn.setEnabled(false);
        pathField.setEnabled(false);
        versionBox.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setVisible(true);
        statusLabel.setText("Đang chuẩn bị tải xuống...");

        // Download in background thread
        new Thread(() -> {
            try {
                File targetFile = new File(modsDir, FILE_NAME);
                downloadFileWithProgress(selected.downloadUrl, targetFile, (bytesRead, totalBytes) -> {
                    SwingUtilities.invokeLater(() -> {
                        if (totalBytes > 0) {
                            int pct = (int) (bytesRead * 100 / totalBytes);
                            progressBar.setValue(pct);
                            statusLabel.setText(String.format("Đang tải: %d%% (%s / %s)", 
                                pct, formatSize(bytesRead), formatSize(totalBytes)));
                        } else {
                            statusLabel.setText("Đang tải: " + formatSize(bytesRead));
                        }
                    });
                });

                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    statusLabel.setText("Cài đặt hoàn tất!");
                    JOptionPane.showMessageDialog(this, 
                        "Đã cài đặt thành công " + selected.name + "\ntại: " + targetFile.getAbsolutePath(),
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    resetUIState();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Lỗi: Tải xuống thất bại.");
                    JOptionPane.showMessageDialog(this, 
                        "Lỗi trong quá trình tải xuống:\n" + e.getMessage(),
                        "Lỗi tải file", JOptionPane.ERROR_MESSAGE);
                    resetUIState();
                });
            }
        }).start();
    }

    private void resetUIState() {
        installBtn.setEnabled(true);
        browseBtn.setEnabled(true);
        pathField.setEnabled(true);
        versionBox.setEnabled(true);
        progressBar.setVisible(false);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.2f %cB", bytes / Math.pow(1024, exp), pre);
    }

    interface ProgressCallback {
        void onProgress(long bytesRead, long totalBytes);
    }

    private static void downloadFileWithProgress(String urlStr, File targetFile, ProgressCallback callback) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "MahikariUpdater/2.0");
        
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestProperty("User-Agent", "MahikariUpdater/2.0");
        }
        
        if (conn.getResponseCode() != 200) {
            throw new IOException("HTTP " + conn.getResponseCode());
        }
        
        long contentLength = conn.getContentLengthLong();
        
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (callback != null) {
                    callback.onProgress(totalRead, contentLength);
                }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Keep default
            }
            new MahikariUpdater().setVisible(true);
        });
    }
}
