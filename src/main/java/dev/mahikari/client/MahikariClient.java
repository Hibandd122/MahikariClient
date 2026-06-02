package dev.mahikari.client;

import dev.mahikari.client.KeyBindHandler;
import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.TeamViewManager;
import dev.mahikari.client.animation.AnimTime;
import dev.mahikari.client.network.ApolloNetworking;
import dev.mahikari.client.network.TeamViewNetworking;
import dev.mahikari.client.render.ProjectionCapture;
import dev.mahikari.client.render.TeamHudRenderer;
import dev.mahikari.client.render.TeamViewRenderer;
import dev.mahikari.client.render.EffectHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import dev.mahikari.client.notification.ChatListener;
import dev.mahikari.client.render.NotificationHudRenderer;
import dev.mahikari.client.render.NametagRenderer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
public class MahikariClient implements ClientModInitializer {
    public static final TeamViewManager MANAGER = new TeamViewManager();
    public static final net.minecraft.client.option.KeyBinding.Category MAIN_CATEGORY = net.minecraft.client.option.KeyBinding.Category.create(net.minecraft.util.Identifier.of("mahikari-client", "main"));

    @Override
    public void onInitializeClient() {
        TeamViewConfig.load();
        HudRenderCallback.EVENT.register((ctx, tick) -> AnimTime.tick());
        
        // Auto-sprint is wired up via KeyBindingSprintMixin — no tick handler needed.

        TeamViewNetworking.registerClient();
        ApolloNetworking.registerClient();

        KeyBindHandler.register();
        ProjectionCapture.register();
        TeamViewRenderer.register();
        TeamHudRenderer.register();
        EffectHudRenderer.register();
        ChatListener.register();
        NotificationHudRenderer.register();
        NametagRenderer.register();
        dev.mahikari.client.update.UpdateManager.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("testteam").executes(context -> {
                net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                if (mc.player != null) {
                    double x = mc.player.getX();
                    double y = mc.player.getY();
                    double z = mc.player.getZ();
                    String world = mc.world.getRegistryKey().getValue().getPath();
                    MANAGER.updatePosition("TestPlayer1", world, x + 5, y, z + 5, "king", "");
                    MANAGER.updatePosition("TestPlayer2", world, x - 5, y, z - 5, "party", "");
                    MANAGER.updatePosition("TestPlayer3", world, x + 10, y, z, "", "");
                    MANAGER.updateHealth("TestPlayer1", 20.0f, 20.0f, 0.0f);
                    MANAGER.updateHealth("TestPlayer2", 10.0f, 20.0f, 0.0f);
                    MANAGER.updateHealth("TestPlayer3", 1.0f, 20.0f, 0.0f);
                    context.getSource().sendFeedback(net.minecraft.text.Text.literal("§aĐã tạo các đồng đội giả để test!"));
                }
                return 1;
            }));
            
            dispatcher.register(ClientCommandManager.literal("cleartestteam").executes(context -> {
                MANAGER.clearAll();
                context.getSource().sendFeedback(net.minecraft.text.Text.literal("§cĐã xóa toàn bộ đồng đội giả!"));
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("testglow").executes(context -> {
                TeamViewManager.TeammateData td1 = MANAGER.findByName("TestPlayer1");
                TeamViewManager.TeammateData td2 = MANAGER.findByName("TestPlayer2");
                TeamViewManager.TeammateData td3 = MANAGER.findByName("TestPlayer3");
                
                if (td1 != null) td1.updateApolloColor(new java.awt.Color(255, 80, 80));   // Red
                if (td2 != null) td2.updateApolloColor(new java.awt.Color(80, 255, 80));   // Green
                if (td3 != null) td3.updateApolloColor(new java.awt.Color(180, 80, 255));  // Purple
                
                context.getSource().sendFeedback(net.minecraft.text.Text.literal("§dĐã áp dụng màu Glow (Apollo Color) giả cho các TestPlayer!"));
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("testdamage").executes(context -> {
                for (TeamViewManager.TeammateData td : MANAGER.getAll()) {
                    if (td.getName().startsWith("TestPlayer")) {
                        float newHp = Math.max(0, td.getHealth() - 4.0f);
                        td.updateHealth(newHp, 20.0f, 0.0f);
                    }
                }
                context.getSource().sendFeedback(net.minecraft.text.Text.literal("§cĐã trừ 4 HP của toàn bộ đồng đội giả!"));
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("testheal").executes(context -> {
                for (TeamViewManager.TeammateData td : MANAGER.getAll()) {
                    if (td.getName().startsWith("TestPlayer")) {
                        float newHp = Math.min(20.0f, td.getHealth() + 4.0f);
                        td.updateHealth(newHp, 20.0f, 0.0f);
                    }
                }
                context.getSource().sendFeedback(net.minecraft.text.Text.literal("§aĐã cộng 4 HP cho toàn bộ đồng đội giả!"));
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("testnotify").executes(context -> {
                dev.mahikari.client.notification.NotificationManager.addNotification("§6§lLEGENDARY CÓ THỂ CRAFT", "Đã craft thành công Dragon Armor!", 0xFFCC00, 8000);
                dev.mahikari.client.notification.NotificationManager.addNotification("§b§lAIRDROP", "Airdrop đã rơi tại 100, 200", 0x00CCFF, 8000);
                context.getSource().sendFeedback(net.minecraft.text.Text.literal("§aĐã gửi thông báo giả (8 giây)! Gõ /clearnotify để xóa."));
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("clearnotify").executes(context -> {
                dev.mahikari.client.notification.NotificationManager.clear();
                context.getSource().sendFeedback(net.minecraft.text.Text.literal("§cĐã xóa toàn bộ thông báo hiển thị!"));
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("edithud").executes(context -> {
                net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                mc.send(() -> {
                    mc.setScreen(new dev.mahikari.client.screen.HudEditorScreen(mc.currentScreen));
                });
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("opensettings").executes(context -> {
                net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                mc.send(() -> {
                    try {
                        mc.setScreen(new dev.mahikari.client.screen.MahikariClickGui(mc.currentScreen));
                        context.getSource().sendFeedback(net.minecraft.text.Text.literal("§aĐã mở settings GUI!"));
                    } catch (Exception e) {
                        context.getSource().sendError(net.minecraft.text.Text.literal("§cLỗi khi mở GUI: " + e.getMessage()));
                        e.printStackTrace();
                    }
                });
                return 1;
            }));
        });
    }
}
