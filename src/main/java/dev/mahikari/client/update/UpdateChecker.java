package dev.mahikari.client.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UpdateChecker {
    public static final String OWNER = "Hibandd122";
    public static final String REPO = "MahikariClient";
    private static final String API_URL = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";
    private static final String USER_AGENT = "MahikariClient-Updater";

    /** Info about a single downloadable asset in a release. */
    public record AssetInfo(String name, String downloadUrl, long sizeBytes) {}

    /** Full release info including all downloadable JARs. */
    public record UpdateInfo(String version, String downloadUrl, String releaseUrl, long sizeBytes,
                             List<AssetInfo> allAssets, String changelog) {}

    public static CompletableFuture<UpdateInfo> checkLatest() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/vnd.github+json")
                        .timeout(Duration.ofSeconds(10))
                        .GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) return null;

                JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                if (!json.has("tag_name")) return null;
                String tagName = json.get("tag_name").getAsString();
                String releaseUrl = json.has("html_url") ? json.get("html_url").getAsString() : "";
                String changelog = json.has("body") && !json.get("body").isJsonNull()
                        ? json.get("body").getAsString() : "";

                String primaryDownloadUrl = null;
                long primarySize = 0;
                List<AssetInfo> allAssets = new ArrayList<>();

                if (json.has("assets")) {
                    JsonArray assets = json.getAsJsonArray("assets");
                    for (JsonElement el : assets) {
                        JsonObject a = el.getAsJsonObject();
                        String name = a.get("name").getAsString();
                        String lower = name.toLowerCase();
                        if (lower.endsWith(".jar")) {
                            // UIMahikari (modid-*.jar) đã được bundle vào mahikari-client.jar — bỏ qua
                            if (lower.startsWith("modid-")) continue;

                            String url = a.get("browser_download_url").getAsString();
                            long size = a.has("size") ? a.get("size").getAsLong() : 0;
                            allAssets.add(new AssetInfo(name, url, size));

                            // Primary = the mahikari-client JAR
                            if (primaryDownloadUrl == null || lower.contains("mahikari")) {
                                primaryDownloadUrl = url;
                                primarySize = size;
                            }
                        }
                    }
                }
                if (primaryDownloadUrl == null) return null;

                return new UpdateInfo(tagName, primaryDownloadUrl, releaseUrl, primarySize, allAssets, changelog);
            } catch (Exception e) {
                return null;
            }
        });
    }

    public static boolean isNewer(String latestVersion, String currentVersion) {
        return compareVersions(strip(latestVersion), strip(currentVersion)) > 0;
    }

    private static String strip(String v) {
        if (v == null) return "0";
        v = v.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        return v;
    }

    private static int compareVersions(String a, String b) {
        String[] aParts = a.split("[.\\-+]");
        String[] bParts = b.split("[.\\-+]");
        int len = Math.max(aParts.length, bParts.length);
        for (int i = 0; i < len; i++) {
            int aNum = i < aParts.length ? parseIntSafe(aParts[i]) : 0;
            int bNum = i < bParts.length ? parseIntSafe(bParts[i]) : 0;
            if (aNum != bNum) return Integer.compare(aNum, bNum);
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) if (c >= '0' && c <= '9') sb.append(c);
        if (sb.length() == 0) return 0;
        try { return Integer.parseInt(sb.toString()); } catch (Exception e) { return 0; }
    }

    /**
     * Download a single JAR, reporting progress via progressCallback (0.0 – 1.0).
     * Uses a temp file (.part) then moves atomically.
     */
    public static CompletableFuture<Path> downloadJar(String url, Path destDir, String fileName) {
        return downloadJar(url, destDir, fileName, null);
    }

    public static CompletableFuture<Path> downloadJar(String url, Path destDir, String fileName, Consumer<Double> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(destDir);
                Path tmp = destDir.resolve(fileName + ".part");
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", USER_AGENT)
                        .timeout(Duration.ofMinutes(5))
                        .GET().build();

                if (progressCallback != null) {
                    // Use InputStream to track progress
                    HttpResponse<java.io.InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException("HTTP " + resp.statusCode());
                    }
                    long contentLength = resp.headers().firstValueAsLong("Content-Length").orElse(-1);
                    try (java.io.InputStream in = resp.body();
                         java.io.OutputStream out = Files.newOutputStream(tmp)) {
                        byte[] buf = new byte[8192];
                        long totalRead = 0;
                        int read;
                        while ((read = in.read(buf)) != -1) {
                            out.write(buf, 0, read);
                            totalRead += read;
                            if (contentLength > 0) {
                                progressCallback.accept((double) totalRead / contentLength);
                            }
                        }
                    }
                } else {
                    HttpResponse<Path> resp = client.send(req, HttpResponse.BodyHandlers.ofFile(tmp));
                    if (resp.statusCode() != 200) {
                        Files.deleteIfExists(tmp);
                        throw new RuntimeException("HTTP " + resp.statusCode());
                    }
                }

                Path finalPath = destDir.resolve(fileName);
                Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING);
                return finalPath;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Download all JAR assets from a release. Deletes old JARs that match known patterns.
     */
    public static CompletableFuture<List<Path>> downloadAllAssets(UpdateInfo info, Path modsDir,
                                                                   Consumer<String> statusCallback,
                                                                   Consumer<Double> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            List<Path> downloaded = new ArrayList<>();
            try {
                // Step 1: Delete old JARs matching known patterns
                if (statusCallback != null) statusCallback.accept("Đang xóa bản cũ...");
                deleteOldJars(modsDir);

                // Step 2: Download each new JAR
                List<AssetInfo> jars = info.allAssets();
                long totalSize = jars.stream().mapToLong(AssetInfo::sizeBytes).sum();
                long[] globalDownloaded = {0};

                for (int i = 0; i < jars.size(); i++) {
                    AssetInfo asset = jars.get(i);
                    if (statusCallback != null) {
                        statusCallback.accept("Đang tải " + (i + 1) + "/" + jars.size() + ": " + asset.name());
                    }

                    final long prevTotal = globalDownloaded[0];
                    Path p = downloadJar(asset.downloadUrl(), modsDir, asset.name(), progress -> {
                        if (progressCallback != null && totalSize > 0) {
                            double current = prevTotal + progress * asset.sizeBytes();
                            progressCallback.accept(current / totalSize);
                        }
                    }).join();

                    globalDownloaded[0] += asset.sizeBytes();
                    downloaded.add(p);
                }

                return downloaded;
            } catch (Exception e) {
                // Clean up partially downloaded files on failure
                for (Path p : downloaded) {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                }
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Delete old Mahikari JARs from the mods directory. Matches known patterns:
     * - mahikari-client-*.jar
     * - modid-1.0.0.jar (UIMahikari — đã bundle vào mahikari-client.jar, xóa để tránh duplicate)
     */
    private static void deleteOldJars(Path modsDir) {
        try {
            if (!Files.isDirectory(modsDir)) return;
            Files.list(modsDir).forEach(p -> {
                String name = p.getFileName().toString().toLowerCase();
                
                // Xóa các file .old từ lần cập nhật trước
                if (name.endsWith(".old")) {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    return;
                }
                
                if (name.endsWith(".jar")) {
                    if (name.startsWith("mahikari-client-") || name.equals("modid-1.0.0.jar")) {
                        try {
                            // Don't delete .part files (in-progress downloads)
                            if (!name.endsWith(".part")) {
                                Files.deleteIfExists(p);
                            }
                        } catch (Exception e) {
                            // On Windows, locked jars cannot be deleted. Rename to .old so Fabric ignores it next time.
                            try {
                                Files.move(p, p.resolveSibling(p.getFileName().toString() + ".old"), StandardCopyOption.REPLACE_EXISTING);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            });
        } catch (Exception ignored) {}
    }
}
