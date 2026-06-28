package dev.mahikari.client.screen;

import dev.mahikari.client.MahikariClient;
import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.TeamViewManager;
import dev.mahikari.client.render.NotificationHudRenderer;
import dev.mahikari.client.render.TeamHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends Screen {
    private static final String[][] PREVIEWS = new String[][] {
            { "PREVIEW",           "Đây là một thông báo mẫu" },
            { "CHẾ TẠO HUYỀN THOẠI", "Player vừa chế tạo Item" },
            { "THÍNH RƠI",         "Tọa độ: X:110 Y:83 Z:-39" },
    };
    private static final int[] PREVIEW_COLORS = new int[] { 0x7BA8FF, 0xFFD24A, 0xFF6B6B };

    private final Screen parent;
    private boolean addedFakeTeammates;

    private int currentTab = 0; // 0: Layout, 1: Settings
    private static final String[] TAB_NAMES = {"Vị trí / Layout", "Cài đặt chung / Settings"};

    private DraggableElement teamHudElement;
    private DraggableElement notificationElement;
    private DraggableElement sprintingElement;
    private DraggableElement effectHudElement;

    private ButtonWidget resetBtn;
    private ButtonWidget doneBtn;

    private double lastMouseX;
    private double lastMouseY;
    private int nudgeCooldown = 0;
    private float animTabX = -1f;
    private float animTabW = -1f;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Edit HUD Layout"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        TeamViewManager mgr = MahikariClient.MANAGER;
        if (mgr.getAll().isEmpty()) {
            mgr.addTeammate("Preview1");
            mgr.updatePosition("Preview1", "", 0.0, 64.0, 0.0, "plains", "king");
            mgr.updateHealth("Preview1", 18.0f, 20.0f, 4.0f);
            mgr.addTeammate("Preview2");
            mgr.updatePosition("Preview2", "", 0.0, 64.0, 0.0, "plains", "vip");
            mgr.updateHealth("Preview2", 12.0f, 20.0f, 0.0f);
            mgr.addTeammate("Preview3");
            mgr.updatePosition("Preview3", "", 0.0, 64.0, 0.0, "forest", "team");
            mgr.updateHealth("Preview3", 6.0f, 20.0f, 0.0f);
            this.addedFakeTeammates = true;
        }

        TeamViewConfig cfg = TeamViewConfig.get();
        int nw = totalNotifyW();
        int nh = totalNotifyH();

        this.teamHudElement = new DraggableElement("Team HUD") {
            @Override int getX() { return cfg.teamHudOffsetX; }
            @Override void setX(int x) { cfg.teamHudOffsetX = x; }
            @Override int getY() { return cfg.teamHudOffsetY; }
            @Override void setY(int y) { cfg.teamHudOffsetY = y; }
            @Override float getScale() { return cfg.teamHudScale; }
            @Override void setScale(float scale) { cfg.teamHudScale = scale; }
            @Override void reset() {
                cfg.teamHudOffsetX = 8;
                cfg.teamHudOffsetY = 8;
                cfg.teamHudScale = 1.0f;
            }
        };
        this.teamHudElement.width = TeamHudRenderer.getCardWidth();
        this.teamHudElement.height = 70;

        this.notificationElement = new DraggableElement("Notifications") {
            @Override int getX() { 
                int ww = (int) (this.width * getScale());
                if (cfg.notificationOffsetX == 9999 || cfg.notificationOffsetX < 0) {
                    return HudEditorScreen.this.width - ww - 10;
                }
                return Math.max(0, Math.min(HudEditorScreen.this.width - ww, cfg.notificationOffsetX));
            }
            @Override void setX(int x) { cfg.notificationOffsetX = x; }
            @Override int getY() { return cfg.notificationOffsetY; }
            @Override void setY(int y) { cfg.notificationOffsetY = y; }
            @Override float getScale() { return cfg.notificationScale; }
            @Override void setScale(float scale) { cfg.notificationScale = scale; }
            @Override void reset() {
                int ww = totalNotifyW();
                cfg.notificationOffsetX = Math.max(0, HudEditorScreen.this.width - ww - 9);
                cfg.notificationOffsetY = 10;
                cfg.notificationScale = 1.0f;
            }
        };
        this.notificationElement.width = nw;
        this.notificationElement.height = nh;

        this.sprintingElement = new DraggableElement("Sprinting Text") {
            @Override int getX() { 
                String prefix = cfg.sprintingShowBorder ? "[SPRINTING: " : "SPRINTING: ";
                String suffix = cfg.sprintingShowBorder ? "]" : "";
                int w = MinecraftClient.getInstance().textRenderer.getWidth(prefix + "ON" + suffix);
                return cfg.sprintingOffsetX < 0 ? HudEditorScreen.this.width - w - 2 : cfg.sprintingOffsetX; 
            }
            @Override void setX(int x) { cfg.sprintingOffsetX = x; }
            @Override int getY() { 
                return cfg.sprintingOffsetY < 0 ? HudEditorScreen.this.height - 12 : cfg.sprintingOffsetY; 
            }
            @Override void setY(int y) { cfg.sprintingOffsetY = y; }
            @Override float getScale() { return cfg.sprintingScale; }
            @Override void setScale(float scale) { cfg.sprintingScale = scale; }
            @Override void reset() {
                cfg.sprintingOffsetX = -1;
                cfg.sprintingOffsetY = -1;
                cfg.sprintingScale = 1.0f;
            }
        };


        this.effectHudElement = new DraggableElement("Effect Status") {
            @Override int getX() { return cfg.effectHudOffsetX; }
            @Override void setX(int x) { cfg.effectHudOffsetX = x; }
            @Override int getY() { return cfg.effectHudOffsetY; }
            @Override void setY(int y) { cfg.effectHudOffsetY = y; }
            @Override float getScale() { return cfg.effectHudScale; }
            @Override void setScale(float scale) { cfg.effectHudScale = scale; }
            @Override void reset() {
                cfg.effectHudOffsetX = 10;
                cfg.effectHudOffsetY = 100;
                cfg.effectHudScale = 1.0f;
            }
        };
        this.effectHudElement.width = 130;
        this.effectHudElement.height = 60; // 2 effects preview, matches EffectHudRenderer

        this.resetBtn = ButtonWidget.builder(Text.literal("Reset Default"), b -> {
            cfg.teamHudOffsetX = 8;
            cfg.teamHudOffsetY = 8;
            cfg.teamHudScale = 1.0f;
            int ww = totalNotifyW();
            cfg.notificationOffsetX = Math.max(0, this.width - ww - 9);
            cfg.notificationOffsetY = 10;
            cfg.notificationScale = 1.0f;
        }).dimensions(this.width / 2 - 60, this.height / 2 - 10, 120, 20).build();
        this.resetBtn.visible = (this.currentTab == 1);
        addDrawableChild(this.resetBtn);

        this.doneBtn = ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width / 2 - 60, this.height - 30, 120, 20).build();
        addDrawableChild(this.doneBtn);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0x44000000, 0x88000000);
        ctx.fill(0, 0, this.width, 40, 0x55000000);
        ctx.fill(0, this.height - 40, this.width, this.height, 0x55000000);
    }

    @Override
    public void renderDarkening(DrawContext ctx) {}

    @Override
    public boolean shouldPause() { return false; }

    private int totalNotifyH() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int h = 0;
        for (int i = 0; i < PREVIEWS.length; i++) {
            int[] sz = NotificationHudRenderer.measureCard(mc, PREVIEWS[i][0], PREVIEWS[i][1]);
            h += sz[1];
            if (i < PREVIEWS.length - 1) h += 10;
        }
        return h;
    }

    private int totalNotifyW() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int maxW = 0;
        for (String[] p : PREVIEWS) {
            int[] sz = NotificationHudRenderer.measureCard(mc, p[0], p[1]);
            if (sz[0] > maxW) maxW = sz[0];
        }
        return maxW;
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.buttonInfo().button();

        int totalWidth = 0;
        int gap = 30;
        for (String name : TAB_NAMES) {
            totalWidth += this.textRenderer.getWidth(name);
        }
        totalWidth += gap * (TAB_NAMES.length - 1);
        int startX = this.width / 2 - totalWidth / 2;
        int y = 15;

        if (button == 0) {
            for (int i = 0; i < TAB_NAMES.length; i++) {
                int tw = this.textRenderer.getWidth(TAB_NAMES[i]);
                int th = this.textRenderer.fontHeight;
                if (mouseX >= startX - 5 && mouseX <= startX + tw + 5 && mouseY >= y - 5 && mouseY <= y + th + 5) {
                    this.currentTab = i;
                    this.resetBtn.visible = (this.currentTab == 1);
                    return true;
                }
                startX += tw + gap;
            }

            if (this.currentTab == 0) {
                if (teamHudElement.isResetHovered(mouseX, mouseY)) {
                    teamHudElement.reset();
                    return true;
                } else if (teamHudElement.isHovered(mouseX, mouseY)) {
                    teamHudElement.dragging = true;
                    teamHudElement.dragOffsetX = (int) (mouseX - teamHudElement.getX());
                    teamHudElement.dragOffsetY = (int) (mouseY - teamHudElement.getY());
                    return true;
                }

                if (notificationElement.isResetHovered(mouseX, mouseY)) {
                    notificationElement.reset();
                    return true;
                } else if (notificationElement.isHovered(mouseX, mouseY)) {
                    notificationElement.dragging = true;
                    notificationElement.dragOffsetX = (int) (mouseX - notificationElement.getX());
                    notificationElement.dragOffsetY = (int) (mouseY - notificationElement.getY());
                    return true;
                }

                if (sprintingElement.isResetHovered(mouseX, mouseY)) {
                    sprintingElement.reset();
                    return true;
                } else if (sprintingElement.isHovered(mouseX, mouseY)) {
                    sprintingElement.dragging = true;
                    sprintingElement.dragOffsetX = (int) (mouseX - sprintingElement.getX());
                    sprintingElement.dragOffsetY = (int) (mouseY - sprintingElement.getY());
                    return true;
                }

                if (effectHudElement.isResetHovered(mouseX, mouseY)) {
                    effectHudElement.reset();
                    return true;
                } else if (effectHudElement.isHovered(mouseX, mouseY)) {
                    effectHudElement.dragging = true;
                    effectHudElement.dragOffsetX = (int) (mouseX - effectHudElement.getX());
                    effectHudElement.dragOffsetY = (int) (mouseY - effectHudElement.getY());
                    return true;
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.buttonInfo().button();

        java.util.List<DraggableElement> others = java.util.Arrays.asList(teamHudElement, notificationElement, sprintingElement, effectHudElement);

        if (this.currentTab == 0) {
            if (teamHudElement.dragging) {
                teamHudElement.snapAndSetPosition((int) (mouseX - teamHudElement.dragOffsetX), (int) (mouseY - teamHudElement.dragOffsetY), this.width, this.height, others);
                return true;
            } else if (notificationElement.dragging) {
                notificationElement.snapAndSetPosition((int) (mouseX - notificationElement.dragOffsetX), (int) (mouseY - notificationElement.dragOffsetY), this.width, this.height, others);
                return true;
            } else if (sprintingElement.dragging) {
                sprintingElement.snapAndSetPosition((int) (mouseX - sprintingElement.dragOffsetX), (int) (mouseY - sprintingElement.dragOffsetY), this.width, this.height, others);
                return true;
            } else if (effectHudElement.dragging) {
                effectHudElement.snapAndSetPosition((int) (mouseX - effectHudElement.dragOffsetX), (int) (mouseY - effectHudElement.dragOffsetY), this.width, this.height, others);
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.buttonInfo().button();

        if (button == 0) {
            boolean wasDragging = teamHudElement.dragging || notificationElement.dragging || sprintingElement.dragging || effectHudElement.dragging;
            teamHudElement.dragging = false;
            notificationElement.dragging = false;
            sprintingElement.dragging = false;
            effectHudElement.dragging = false;
            if (wasDragging) {
                TeamViewConfig.save();
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.currentTab == 0 && verticalAmount != 0) {
            if (teamHudElement.isHovered(mouseX, mouseY)) {
                float currentScale = teamHudElement.getScale();
                if (verticalAmount > 0) teamHudElement.setScale(Math.min(2.0f, currentScale + 0.05f));
                else teamHudElement.setScale(Math.max(0.5f, currentScale - 0.05f));
                return true;
            } else if (notificationElement.isHovered(mouseX, mouseY)) {
                float currentScale = notificationElement.getScale();
                if (verticalAmount > 0) notificationElement.setScale(Math.min(2.5f, currentScale + 0.05f));
                else notificationElement.setScale(Math.max(0.5f, currentScale - 0.05f));
                return true;
            } else if (effectHudElement.isHovered(mouseX, mouseY)) {
                float currentScale = effectHudElement.getScale();
                if (verticalAmount > 0) effectHudElement.setScale(Math.min(2.5f, currentScale + 0.05f));
                else effectHudElement.setScale(Math.max(0.5f, currentScale - 0.05f));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        
        super.render(ctx, mouseX, mouseY, delta);
        
        renderTabs(ctx, mouseX, mouseY, delta);

        if (this.currentTab == 0) {
            handleNudge(mouseX, mouseY);
            renderLayoutTab(ctx, mouseX, mouseY, delta);
        } else {
            renderSettingsTab(ctx, mouseX, mouseY);
        }
    }

    private void handleNudge(int mouseX, int mouseY) {
        if (this.nudgeCooldown > 0) {
            this.nudgeCooldown--;
            return;
        }

        long window = MinecraftClient.getInstance().getWindow().getHandle();
        int dx = 0;
        int dy = 0;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) dy = -1;
        else if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) dy = 1;
        else if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) dx = -1;
        else if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) dx = 1;

        if (dx != 0 || dy != 0) {
            java.util.List<DraggableElement> others = java.util.Arrays.asList(teamHudElement, notificationElement, sprintingElement, effectHudElement);
            if (teamHudElement.isHovered(mouseX, mouseY)) {
                teamHudElement.nudge(dx, dy, this.width, this.height, others);
                this.nudgeCooldown = 3;
            } else if (notificationElement.isHovered(mouseX, mouseY)) {
                notificationElement.nudge(dx, dy, this.width, this.height, others);
                this.nudgeCooldown = 3;
            } else if (sprintingElement.isHovered(mouseX, mouseY)) {
                sprintingElement.nudge(dx, dy, this.width, this.height, others);
                this.nudgeCooldown = 3;
            } else if (effectHudElement.isHovered(mouseX, mouseY)) {
                effectHudElement.nudge(dx, dy, this.width, this.height, others);
                this.nudgeCooldown = 3;
            }
        }
    }

    private void renderTabs(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int totalWidth = 0;
        int gap = 30;
        for (String name : TAB_NAMES) {
            totalWidth += this.textRenderer.getWidth(name);
        }
        totalWidth += gap * (TAB_NAMES.length - 1);

        int startX = this.width / 2 - totalWidth / 2;
        int y = 15;

        for (int i = 0; i < TAB_NAMES.length; i++) {
            String name = TAB_NAMES[i];
            int tw = this.textRenderer.getWidth(name);
            int th = this.textRenderer.fontHeight;
            boolean hovered = mouseX >= startX - 5 && mouseX <= startX + tw + 5 && mouseY >= y - 5 && mouseY <= y + th + 5;

            if (i == this.currentTab) {
                ctx.drawTextWithShadow(this.textRenderer, name, startX, y, 0xFFFFFFFF);
            } else {
                int color = hovered ? 0xFFAAAAAA : 0xFF777777;
                ctx.drawTextWithShadow(this.textRenderer, name, startX, y, color);
            }
            startX += tw + gap;
        }

        // Animated underline
        int tWidth = 0;
        int tX = this.width / 2 - totalWidth / 2;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int tw = this.textRenderer.getWidth(TAB_NAMES[i]);
            if (i == this.currentTab) {
                tWidth = tw;
                break;
            }
            tX += tw + gap;
        }

        if (this.animTabX < 0) {
            this.animTabX = tX;
            this.animTabW = tWidth;
        } else {
            this.animTabX += (tX - this.animTabX) * Math.min(1.0f, delta * 0.4f);
            this.animTabW += (tWidth - this.animTabW) * Math.min(1.0f, delta * 0.4f);
        }
        
        int th = this.textRenderer.fontHeight;
        ctx.fill((int)this.animTabX, y + th + 2, (int)(this.animTabX + this.animTabW), y + th + 3, 0xFFFFAA00);
    }

    private void drawDashedLineV(DrawContext ctx, int x, int y1, int y2, int color) {
        for (int y = y1; y < y2; y += 8) {
            ctx.fill(x, y, x + 1, Math.min(y + 4, y2), color);
        }
    }
    
    private void drawDashedLineH(DrawContext ctx, int x1, int x2, int y, int color) {
        for (int x = x1; x < x2; x += 8) {
            ctx.fill(x, y, Math.min(x + 4, x2), y + 1, color);
        }
    }

    private void renderLayoutTab(DrawContext ctx, int mouseX, int mouseY, float delta) {
        TeamViewConfig cfg = TeamViewConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();

        int nx = notificationElement.getX();
        int ny = notificationElement.getY();
        int baseW = totalNotifyW();
        int baseH = totalNotifyH();
        int scaledNW = (int) (baseW * cfg.notificationScale);
        
        boolean fromRight = (nx + scaledNW / 2) > this.width / 2;

        // Dot grid background
        int dotColor = 0x22FFFFFF;
        for (int i = 0; i < this.width; i += 20) {
            for (int j = 40; j < this.height - 40; j += 20) {
                ctx.fill(i, j, i + 1, j + 1, dotColor);
            }
        }

        Matrix3x2fStack matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(nx, ny);
        matrices.scale(cfg.notificationScale, cfg.notificationScale);

        int yOffset = 0;
        int localRightEdge = baseW;
        for (int i = 0; i < PREVIEWS.length; i++) {
            int[] sz = NotificationHudRenderer.measureCard(mc, PREVIEWS[i][0], PREVIEWS[i][1]);
            int sx = fromRight ? (localRightEdge - sz[0]) : 0;
            NotificationHudRenderer.drawCard(ctx, mc, PREVIEWS[i][0], PREVIEWS[i][1], PREVIEW_COLORS[i], 0.7f - i * 0.15f,
                    sx, yOffset, sz[0], sz[1], 255);
            yOffset += sz[1] + 10;
        }
        matrices.popMatrix();

        int tx = cfg.teamHudOffsetX;
        int ty = cfg.teamHudOffsetY;
        matrices.pushMatrix();
        matrices.translate(tx, ty);
        matrices.scale(cfg.teamHudScale, cfg.teamHudScale);
        
        int tCursor = 0;
        for (int i = 1; i <= 3; i++) {
            TeamViewManager.TeammateData td = MahikariClient.MANAGER.findByName("Preview" + i);
            if (td != null) {
                int cardH = TeamHudRenderer.drawCard(ctx, mc, cfg, td, tCursor);
                tCursor += cardH + 4;
            }
        }
        matrices.popMatrix();
        this.teamHudElement.width = TeamHudRenderer.getCardWidth();
        this.teamHudElement.height = Math.max(70, tCursor - 4);

        DraggableElement draggingEl = null;
        if (teamHudElement.dragging) draggingEl = teamHudElement;
        else if (notificationElement.dragging) draggingEl = notificationElement;
        else if (sprintingElement.dragging) draggingEl = sprintingElement;
        else if (effectHudElement.dragging) draggingEl = effectHudElement;

        if (draggingEl != null) {
            int snapColor = 0xFF00E5FF; // Cyan for center snap
            int edgeColor = 0xFFFF55FF; // Magenta for edge snap
            int otherColor = 0xFF55FF55; // Green for element snap
            
            if (draggingEl.snappedXCenter) ctx.fill(this.width / 2, 0, this.width / 2 + 1, this.height, snapColor);
            if (draggingEl.snappedYCenter) ctx.fill(0, this.height / 2, this.width, this.height / 2 + 1, snapColor);
            if (draggingEl.snappedXEdge) {
                if (draggingEl.getX() == 0) ctx.fill(0, 0, 1, this.height, edgeColor);
                else ctx.fill(this.width - 1, 0, this.width, this.height, edgeColor);
            }
            if (draggingEl.snappedYEdge) {
                if (draggingEl.getY() == 0) ctx.fill(0, 0, this.width, 1, edgeColor);
                else ctx.fill(0, this.height - 1, this.width, this.height, edgeColor);
            }
            
            if (draggingEl.snappedOtherX != -1) {
                drawDashedLineV(ctx, draggingEl.snappedOtherX, 0, this.height, otherColor);
            }
            if (draggingEl.snappedOtherY != -1) {
                drawDashedLineH(ctx, 0, this.width, draggingEl.snappedOtherY, otherColor);
            }
            
            // Draw coordinate pill
            String coords = "[" + draggingEl.getX() + ", " + draggingEl.getY() + "]";
            int cw = this.textRenderer.getWidth(coords);
            int cx = draggingEl.getX() + (int)(draggingEl.width * draggingEl.getScale()) / 2 - cw / 2;
            int cy = draggingEl.getY() - 14;
            if (cy < 0) cy = draggingEl.getY() + (int)(draggingEl.height * draggingEl.getScale()) + 4;
            fillRoundedGradient(ctx, cx - 4, cy - 2, cx + cw + 4, cy + 10, 0xAA000000, 0x88000000, 3);
            ctx.drawTextWithShadow(this.textRenderer, coords, cx, cy, 0xFFFFFF55);
        }

        this.teamHudElement.render(ctx, mouseX, mouseY, delta);
        this.notificationElement.render(ctx, mouseX, mouseY, delta);
        
        String prefix = cfg.sprintingShowBorder ? "[SPRINTING: " : "SPRINTING: ";
        String suffix = cfg.sprintingShowBorder ? "]" : "";
        String sprintText = prefix + "ON" + suffix;
        int sw = mc.textRenderer.getWidth(sprintText);
        
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(sprintingElement.getX(), sprintingElement.getY());
        ctx.getMatrices().scale(sprintingElement.getScale(), sprintingElement.getScale());
        
        if (cfg.sprintingShowBackground) {
            fillRoundedGradient(ctx, -2, -2, sw + 2, mc.textRenderer.fontHeight + 2, 0x881A202C, 0x550D1117, 2);
        }
        ctx.drawTextWithShadow(this.textRenderer, sprintText, 0, 0, cfg.getSprintingColorARGB());
        ctx.getMatrices().popMatrix();

        this.sprintingElement.width = sw;
        this.sprintingElement.height = mc.textRenderer.fontHeight;

        this.sprintingElement.render(ctx, mouseX, mouseY, delta);

        // Preview Effect HUD
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(effectHudElement.getX(), effectHudElement.getY());
        ctx.getMatrices().scale(effectHudElement.getScale(), effectHudElement.getScale());
        
        // Mock effects preview
        boolean isHorizontal = "HORIZONTAL".equalsIgnoreCase(cfg.effectHudLayout);
        int ew = 0, eh = 0;
        
        if (isHorizontal) {
            // Must match EffectHudRenderer horizontal mode: box height 28, gap 6, width = textW + 36, accent bar 3px wide y=3..25
            int w1 = Math.max(mc.textRenderer.getWidth("Strength II"), mc.textRenderer.getWidth("1:30")) + 36;
            if (cfg.effectHudShowBackground) {
                fillRoundedGradient(ctx, 0, 0, w1, 28, 0x881A202C, 0x550D1117, 6);
            }
            if (cfg.effectHudShowBorder) {
                ctx.fill(0, 3, 3, 25, 0xFFFF5555);
            }
            ctx.drawTextWithShadow(mc.textRenderer, "Strength II", 30, 6, 0xFFFF5555);
            ctx.drawTextWithShadow(mc.textRenderer, "1:30", 30, 16, 0xFFAAAAAA);

            int w2 = Math.max(mc.textRenderer.getWidth("Speed II"), mc.textRenderer.getWidth("0:45")) + 36;
            int x2 = w1 + 6;
            if (cfg.effectHudShowBackground) {
                fillRoundedGradient(ctx, x2, 0, x2 + w2, 28, 0x881A202C, 0x550D1117, 6);
            }
            if (cfg.effectHudShowBorder) {
                ctx.fill(x2, 3, x2 + 3, 25, 0xFF55FFFF);
            }
            ctx.drawTextWithShadow(mc.textRenderer, "Speed II", x2 + 30, 6, 0xFF55FFFF);
            ctx.drawTextWithShadow(mc.textRenderer, "0:45", x2 + 30, 16, 0xFFFF5555);

            ew = x2 + w2;
            eh = 28;
        } else {
            // Must match EffectHudRenderer vertical mode: box 130x28, yOffset += 32 (gap 4), accent bar 3px wide y=3..25
            if (cfg.effectHudShowBackground) {
                fillRoundedGradient(ctx, 0, 0, 130, 28, 0x881A202C, 0x550D1117, 6);
            }
            if (cfg.effectHudShowBorder) {
                ctx.fill(0, 3, 3, 25, 0xFFFF5555);
            }
            ctx.drawTextWithShadow(mc.textRenderer, "Strength II", 30, 6, 0xFFFF5555);
            ctx.drawTextWithShadow(mc.textRenderer, "1:30", 30, 16, 0xFFAAAAAA);

            if (cfg.effectHudShowBackground) {
                fillRoundedGradient(ctx, 0, 32, 130, 60, 0x881A202C, 0x550D1117, 6);
            }
            if (cfg.effectHudShowBorder) {
                ctx.fill(0, 35, 3, 57, 0xFF55FFFF);
            }
            ctx.drawTextWithShadow(mc.textRenderer, "Speed II", 30, 38, 0xFF55FFFF);
            ctx.drawTextWithShadow(mc.textRenderer, "0:45", 30, 48, 0xFFFF5555);

            ew = 130;
            eh = 60;
        }

        this.effectHudElement.width = ew;
        this.effectHudElement.height = eh;

        ctx.getMatrices().popMatrix();

        this.effectHudElement.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u00A77Kéo các thành phần để sắp xếp. Cuộn chuột để đổi kích thước."), this.width / 2, this.height - 65, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u00A77Dùng Phím Mũi Tên để dịch chuyển tinh chỉnh."), this.width / 2, this.height - 52, 0xFFFFFFFF);
    }

    private void renderSettingsTab(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eCài Đặt Chung"), this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7(Tính năng đang được phát triển)"), this.width / 2, this.height / 2 - 25, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        if (this.addedFakeTeammates) {
            MahikariClient.MANAGER.removeTeammate("Preview1");
            MahikariClient.MANAGER.removeTeammate("Preview2");
            MahikariClient.MANAGER.removeTeammate("Preview3");
        }
        TeamViewConfig.save();
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private static void fillRoundedGradient(DrawContext ctx, int x0, int y0, int x1, int y1, int colorStart, int colorEnd, int r) {
        if (r < 1 || x1 - x0 < 2 * r || y1 - y0 < 2 * r) {
            ctx.fillGradient(x0, y0, x1, y1, colorStart, colorEnd);
            return;
        }
        ctx.fillGradient(x0, y0 + r, x1, y1 - r, colorStart, colorEnd);
        for (int i = 0; i < r; i++) {
            int inset = r - i;
            ctx.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, colorStart);
            ctx.fill(x0 + inset, y1 - 1 - i, x1 - inset, y1 - i, colorEnd);
        }
    }

    private abstract class DraggableElement {
        String name;
        int width, height;
        boolean dragging;
        int dragOffsetX, dragOffsetY;

        DraggableElement(String name) {
            this.name = name;
        }

        abstract int getX();
        abstract void setX(int x);
        abstract int getY();
        abstract void setY(int y);
        abstract float getScale();
        abstract void setScale(float scale);
        abstract void reset();

        boolean snappedXCenter, snappedYCenter;
        boolean snappedXEdge, snappedYEdge;
        int snappedOtherX = -1;
        int snappedOtherY = -1;
        float hoverAlpha = 0f;

        boolean isHovered(double mouseX, double mouseY) {
            int scaledW = (int) (width * getScale());
            int scaledH = (int) (height * getScale());
            return mouseX >= getX() && mouseX <= getX() + scaledW && mouseY >= getY() && mouseY <= getY() + scaledH;
        }

        boolean isResetHovered(double mouseX, double mouseY) {
            int scaledW = (int) (width * getScale());
            int badgeH = textRenderer.fontHeight + 4;
            int resetBadgeW = textRenderer.getWidth("[Reset]") + 6;
            
            int x = getX();
            int y = getY();
            return mouseX >= x + scaledW - resetBadgeW + 1 && mouseX <= x + scaledW + 1 && mouseY >= y - badgeH - 1 && mouseY <= y - 1;
        }

        void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            boolean bodyHovered = isHovered(mouseX, mouseY);
            boolean resetHovered = isResetHovered(mouseX, mouseY);
            
            float targetAlpha = (bodyHovered || resetHovered || dragging) ? 1.0f : 0.0f;
            hoverAlpha += (targetAlpha - hoverAlpha) * Math.min(1.0f, delta * 0.3f);

            if (hoverAlpha < 0.02f) return;

            int alphaInt = (int)(hoverAlpha * 255);
            int bgAlpha = (int)(0x66 * hoverAlpha);
            int bg = (bgAlpha << 24);

            int x = getX();
            int y = getY();
            int scaledW = (int) (width * getScale());
            int scaledH = (int) (height * getScale());

            ctx.fill(x, y, x + scaledW, y + scaledH, bg);

            int borderColor = dragging ? 0xFFFFAA00 : (alphaInt << 24 | 0xFFFFFF);
            
            ctx.fill(x - 1, y - 1, x + scaledW + 1, y, borderColor);
            ctx.fill(x - 1, y + scaledH, x + scaledW + 1, y + scaledH + 1, borderColor);
            ctx.fill(x - 1, y, x, y + scaledH, borderColor);
            ctx.fill(x + scaledW, y, x + scaledW + 1, y + scaledH, borderColor);

            String badgeText = name + " (" + Math.round(getScale() * 100) + "%)";
            int textW = textRenderer.getWidth(badgeText);
            int badgeW = textW + 6;
            int badgeH = textRenderer.fontHeight + 4;
            
            int badgeBgAlpha = (int)(0x99 * hoverAlpha);
            ctx.fill(x - 1, y - badgeH - 1, x - 1 + badgeW, y - 1, (badgeBgAlpha << 24));
            ctx.drawTextWithShadow(textRenderer, badgeText, x + 2, y - badgeH + 1, borderColor);

            String resetText = "[Reset]";
            int resetBadgeW = textRenderer.getWidth(resetText) + 6;
            int resetColorRaw = resetHovered ? 0xFF5555 : 0xAAAAAA;
            int resetColor = (alphaInt << 24) | resetColorRaw;
            
            ctx.fill(x + scaledW - resetBadgeW + 1, y - badgeH - 1, x + scaledW + 1, y - 1, (badgeBgAlpha << 24));
            ctx.drawTextWithShadow(textRenderer, resetText, x + scaledW - resetBadgeW + 4, y - badgeH + 1, resetColor);
        }


        void snapAndSetPosition(int newX, int newY, int screenW, int screenH, java.util.List<DraggableElement> others) {
            int scaledW = (int) (width * getScale());
            int scaledH = (int) (height * getScale());
            int snapDist = 5;

            snappedXCenter = false; snappedYCenter = false;
            snappedXEdge = false; snappedYEdge = false;
            snappedOtherX = -1; snappedOtherY = -1;

            if (Math.abs(newX) < snapDist) { newX = 0; snappedXEdge = true; }
            else if (Math.abs(newX + scaledW - screenW) < snapDist) { newX = screenW - scaledW; snappedXEdge = true; }
            else if (Math.abs(newX + scaledW / 2 - screenW / 2) < snapDist) { newX = screenW / 2 - scaledW / 2; snappedXCenter = true; }
            else {
                for (DraggableElement other : others) {
                    if (other == this) continue;
                    int otherX = other.getX();
                    int otherW = (int) (other.width * other.getScale());
                    if (Math.abs(newX - otherX) < snapDist) { newX = otherX; snappedOtherX = newX; break; }
                    if (Math.abs(newX - (otherX + otherW)) < snapDist) { newX = otherX + otherW; snappedOtherX = newX; break; }
                    if (Math.abs(newX + scaledW - (otherX + otherW)) < snapDist) { newX = otherX + otherW - scaledW; snappedOtherX = otherX + otherW; break; }
                    if (Math.abs(newX + scaledW - otherX) < snapDist) { newX = otherX - scaledW; snappedOtherX = otherX; break; }
                    if (Math.abs((newX + scaledW / 2) - (otherX + otherW / 2)) < snapDist) { newX = otherX + otherW / 2 - scaledW / 2; snappedOtherX = otherX + otherW / 2; break; }
                }
            }

            if (Math.abs(newY) < snapDist) { newY = 0; snappedYEdge = true; }
            else if (Math.abs(newY + scaledH - screenH) < snapDist) { newY = screenH - scaledH; snappedYEdge = true; }
            else if (Math.abs(newY + scaledH / 2 - screenH / 2) < snapDist) { newY = screenH / 2 - scaledH / 2; snappedYCenter = true; }
            else {
                for (DraggableElement other : others) {
                    if (other == this) continue;
                    int otherY = other.getY();
                    int otherH = (int) (other.height * other.getScale());
                    if (Math.abs(newY - otherY) < snapDist) { newY = otherY; snappedOtherY = newY; break; }
                    if (Math.abs(newY - (otherY + otherH)) < snapDist) { newY = otherY + otherH; snappedOtherY = newY; break; }
                    if (Math.abs(newY + scaledH - (otherY + otherH)) < snapDist) { newY = otherY + otherH - scaledH; snappedOtherY = otherY + otherH; break; }
                    if (Math.abs(newY + scaledH - otherY) < snapDist) { newY = otherY - scaledH; snappedOtherY = otherY; break; }
                    if (Math.abs((newY + scaledH / 2) - (otherY + otherH / 2)) < snapDist) { newY = otherY + otherH / 2 - scaledH / 2; snappedOtherY = otherY + otherH / 2; break; }
                }
            }

            setX(Math.max(0, Math.min(screenW - scaledW, newX)));
            setY(Math.max(0, Math.min(screenH - scaledH, newY)));
        }

        void nudge(int dx, int dy, int screenW, int screenH, java.util.List<DraggableElement> others) {
            snapAndSetPosition(getX() + dx, getY() + dy, screenW, screenH, others);
        }
    }
}
