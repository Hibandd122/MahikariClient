package dev.mahikari.client.update;

import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.update.UpdateChecker.UpdateInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.List;

public class UpdatePromptScreen extends Screen {
    private final Screen parent;
    private final UpdateInfo info;
    private final String currentVersion;

    private enum State { PROMPT, DOWNLOADING, DONE, ERROR }
    private State state = State.PROMPT;
    private String errorMsg = "";
    private String statusMsg = "";
    private double downloadProgress = 0.0;
    private List<Path> downloadedFiles;

    private ButtonWidget updateBtn;
    private ButtonWidget skipBtn;
    private ButtonWidget neverBtn;
    private ButtonWidget closeBtn;

    public UpdatePromptScreen(Screen parent, UpdateInfo info, String currentVersion) {
        super(Text.literal("Mahikari Client Update"));
        this.parent = parent;
        this.info = info;
        this.currentVersion = currentVersion;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int btnW = 240;
        int btnH = 20;

        updateBtn = ButtonWidget.builder(Text.literal("§a§lCập nhật ngay"), b -> startDownload())
                .dimensions(cx - btnW / 2, cy + 20, btnW, btnH).build();
        skipBtn = ButtonWidget.builder(Text.literal("Không cập nhật"), b -> closeBack())
                .dimensions(cx - btnW / 2, cy + 45, btnW, btnH).build();
        neverBtn = ButtonWidget.builder(Text.literal("§7Không cập nhật, không hỏi lại"), b -> {
            TeamViewConfig.get().skipUpdateForVersion = info.version();
            TeamViewConfig.save();
            closeBack();
        }).dimensions(cx - btnW / 2, cy + 70, btnW, btnH).build();

        closeBtn = ButtonWidget.builder(Text.literal("Đóng"), b -> closeBack())
                .dimensions(cx - btnW / 2, cy + 70, btnW, btnH).build();
        closeBtn.visible = false;

        addDrawableChild(updateBtn);
        addDrawableChild(skipBtn);
        addDrawableChild(neverBtn);
        addDrawableChild(closeBtn);
    }

    private void closeBack() {
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private void startDownload() {
        state = State.DOWNLOADING;
        updateBtn.visible = false;
        skipBtn.visible = false;
        neverBtn.visible = false;
        statusMsg = "Đang chuẩn bị...";
        downloadProgress = 0.0;

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");

        // Download ALL assets (both mahikari-client and UIMahikari JARs)
        // and auto-delete old versions
        UpdateChecker.downloadAllAssets(
                info,
                modsDir,
                status -> {
                    if (this.client != null) {
                        this.client.execute(() -> this.statusMsg = status);
                    }
                },
                progress -> {
                    if (this.client != null) {
                        this.client.execute(() -> this.downloadProgress = progress);
                    }
                }
        ).whenComplete((paths, err) -> {
            if (this.client == null) return;
            this.client.execute(() -> {
                if (err != null) {
                    state = State.ERROR;
                    errorMsg = err.getCause() != null ? err.getCause().getMessage() : err.getMessage();
                    if (errorMsg == null) errorMsg = "Lỗi tải file";
                    closeBtn.visible = true;
                } else {
                    state = State.DONE;
                    downloadedFiles = paths;
                    closeBtn.visible = true;
                }
            });
        });
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Panel
        int panelW = 360;
        int panelH = 220;
        int panelX = cx - panelW / 2;
        int panelY = cy - panelH / 2 - 10;
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE0101418);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF00D9FF);
        ctx.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF00D9FF);

        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§b§lMahikari Client"), cx, panelY + 12, 0xFFFFFF);

        if (state == State.PROMPT) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eCó bản cập nhật mới"), cx, cy - 50, 0xFFFFFF);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7v" + currentVersion + " §f→ §a" + info.version()), cx, cy - 30, 0xFFFFFF);

            long totalSize = info.allAssets().stream().mapToLong(UpdateChecker.AssetInfo::sizeBytes).sum();
            if (totalSize > 0) {
                int assetCount = info.allAssets().size();
                String sizeText = "§7Dung lượng: §f" + formatSize(totalSize) + " §7(" + assetCount + " file)";
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(sizeText), cx, cy - 15, 0xFFFFFF);
            }
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Tự động xóa bản cũ & tải bản mới"), cx, cy + 5, 0xFFFFFF);
        } else if (state == State.DOWNLOADING) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eĐang tải bản cập nhật..."), cx, cy - 30, 0xFFFFFF);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7" + statusMsg), cx, cy - 14, 0xFFFFFF);

            // Progress bar
            int barW = 260;
            int barH = 12;
            int barX = cx - barW / 2;
            int barY = cy + 4;
            ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1D2E);
            ctx.fill(barX, barY, barX + barW, barY + 1, 0xFF2D3250);
            ctx.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0xFF2D3250);

            int filledW = (int) (barW * Math.min(1.0, downloadProgress));
            if (filledW > 0) {
                ctx.fillGradient(barX, barY + 1, barX + filledW, barY + barH - 1, 0xFF00D9FF, 0xFF0099CC);
            }

            String pctStr = String.format("%.0f%%", downloadProgress * 100);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§f" + pctStr), cx, barY + 2, 0xFFFFFF);
        } else if (state == State.DONE) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§a§lĐã tải xong"), cx, cy - 40, 0xFFFFFF);

            if (downloadedFiles != null) {
                int fileY = cy - 22;
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Đã tải " + downloadedFiles.size() + " file:"), cx, fileY, 0xFFFFFF);
                for (int i = 0; i < downloadedFiles.size(); i++) {
                    String name = downloadedFiles.get(i).getFileName().toString();
                    ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§f• " + name), cx, fileY + 14 + i * 12, 0xFFFFFF);
                }
            }

            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§cKhởi động lại Minecraft để hoàn tất"), cx, cy + 30, 0xFFFFFF);
        } else if (state == State.ERROR) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§c§lLỗi tải file"), cx, cy - 20, 0xFFFFFF);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7" + errorMsg), cx, cy, 0xFFFFFF);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return state != State.DOWNLOADING;
    }
}
