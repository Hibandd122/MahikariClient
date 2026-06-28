package dev.mahikari.client.notification;

import dev.mahikari.client.TeamViewConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener {
    private static final String AIRDROP_NOTIF_TITLE = "THÍNH RƠI";
    private static final String AIRDROP_FALLEN_NOTIF_TITLE = "THÍNH ĐÃ RƠI";

    // Vd: 󰀈 󰍧Khanhthanh19030 đã chế tạo Goldreaper! Không ai có thể...
    // Server prefixes with icon (󰀈) + team icon (󰍧/󰍰/󰍣/󰍩/󰍥) before player name
    private static final Pattern LEGENDARY_CRAFT_PATTERN = Pattern
            .compile("(\\w+)\\s+đã chế tạo\\s+(.+?)(?:!|huyền thoại|$)");

    // Vd: Đã có tín hiệu thính rơi... tọa độ x110, y83, z-39
    private static final Pattern AIRDROP_PATTERN = Pattern
            .compile("(?i)thính rơi.*?[xX](-?\\d+).*?[yY](-?\\d+).*?[zZ](-?\\d+)");

    // Vd: Thính đã bắt đầu rơi!
    private static final Pattern AIRDROP_FALLEN_PATTERN = Pattern
            .compile("(?i)thính đã bắt đầu rơi");

    // Vd: Bạn có đủ nguyên liệu để chế tạo Lumberjack's Axe
    // Also matches: "to craft a Smelter's Pickaxe" (English Hoplite)
    private static final Pattern CRAFTABLE_SINGLE_PATTERN = Pattern
            .compile("(?i)^(?!.*nhấn vào đây|.*click here).*(?:đủ nguyên liệu để chế tạo |to craft an? )(.*)$");

    private static final Pattern CRAFTABLE_L1_PATTERN = Pattern
            .compile("(?i).*(?:đủ nguyên liệu|enough materials).*");

    private static final Pattern CRAFTABLE_L2_PATTERN = Pattern
            .compile("(?i)^(?:để chế tạo|to craft)\\s+(.+)$");

    // English Hoplite Patterns
    private static final Pattern HOPLITE_CRAFT_PATTERN = Pattern
            .compile("(?i)(\\w+)\\s+(?:have|has)\\s+crafted\\s+(?:the\\s+)?(.+?)(?:!|$)");
    private static final Pattern HOPLITE_AIRDROP_PATTERN = Pattern
            .compile("(?i)supply drop is spawning.*?X=(-?\\d+).*?Z=(-?\\d+)");

    private static String lastLine = "";
    private static long lastLineTime = 0L;

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            onMessageReceived(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, instant) -> {
            onMessageReceived(message.getString());
        });
    }

    private static void onMessageReceived(String rawMessage) {
        if (rawMessage == null) return;
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.enabled) return;

        String plainText = rawMessage.trim();
        long now = System.currentTimeMillis();

        // 1. Legendary Item Crafting Alert
        if (cfg.notifyLegendary) {
            Matcher m = LEGENDARY_CRAFT_PATTERN.matcher(plainText);
            if (m.find()) {
                String player = m.group(1);
                String item = m.group(2).trim();
                NotificationManager.addNotification(
                        "CHẾ TẠO HUYỀN THOẠI",
                        player + " vừa chế tạo " + item,
                        0xFFD24A,
                        6000
                );
                return;
            }
            Matcher mHoplite = HOPLITE_CRAFT_PATTERN.matcher(plainText);
            if (mHoplite.find()) {
                String player = mHoplite.group(1);
                String item = mHoplite.group(2).trim();
                NotificationManager.addNotification(
                        "CHẾ TẠO TRANG BỊ",
                        (player.equalsIgnoreCase("You") ? "Bạn" : player) + " chế tạo " + item,
                        0xFFD24A,
                        4000
                );
                return;
            }
        }

        // 2. Airdrop Coordinates
        if (cfg.notifyAirdrop) {
            Matcher m = AIRDROP_PATTERN.matcher(plainText);
            if (m.find()) {
                String x = m.group(1);
                String y = m.group(2);
                String z = m.group(3);
                NotificationManager.addNotification(
                        AIRDROP_NOTIF_TITLE,
                        "Tọa độ: X:" + x + " Y:" + y + " Z:" + z,
                        0xFF6B6B,
                        8000
                );
                return;
            }
            
            Matcher mHoplite = HOPLITE_AIRDROP_PATTERN.matcher(plainText);
            if (mHoplite.find()) {
                String x = mHoplite.group(1);
                String z = mHoplite.group(2);
                NotificationManager.addNotification(
                        AIRDROP_NOTIF_TITLE,
                        "Tọa độ: X:" + x + " Z:" + z,
                        0xFF6B6B,
                        8000
                );
                return;
            }

            Matcher m2 = AIRDROP_FALLEN_PATTERN.matcher(plainText);
            if (m2.find()) {
                NotificationManager.addNotification(
                        AIRDROP_FALLEN_NOTIF_TITLE,
                        "Mau đi loot ngay!",
                        0xFF4A4A,
                        5000
                );
                return;
            }
        }

        // 3. Craftable Alert
        if (cfg.notifyCraftable) {
            // Check single-line first
            Matcher m = CRAFTABLE_SINGLE_PATTERN.matcher(plainText);
            if (m.find()) {
                String item = m.group(1).trim();
                NotificationManager.addNotification(
                        "SẴN SÀNG CHẾ TẠO",
                        "Bạn đã đủ đồ chế tạo " + item,
                        0x7BA8FF,
                        4000
                );
                return;
            }

            // Check multiline
            if (CRAFTABLE_L1_PATTERN.matcher(plainText).matches()) {
                lastLine = plainText;
                lastLineTime = now;
            } else if (now - lastLineTime < 1000) {
                Matcher mL2 = CRAFTABLE_L2_PATTERN.matcher(plainText);
                if (mL2.find()) {
                    String item = mL2.group(1).trim();
                    // Ignore action prompt lines
                    if (!item.toLowerCase().contains("nhấn vào đây") && !item.toLowerCase().contains("click here")) {
                        NotificationManager.addNotification(
                                "SẴN SÀNG CHẾ TẠO",
                                "Bạn đã đủ đồ chế tạo " + item,
                                0x7BA8FF,
                                4000
                        );
                        lastLine = ""; // consume
                        lastLineTime = 0L;
                        return;
                    }
                }
            }
        }
    }
}
