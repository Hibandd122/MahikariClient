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
            .compile("(?:\\S+\\s+)?\\S(\\w+)\\s+đã chế tạo\\s+(.+?)(?:!|huyền thoại)");

    // Vd: Đã có tín hiệu thính rơi... tọa độ x110, y83, z-39
    private static final Pattern AIRDROP_PATTERN = Pattern
            .compile("(?i)thính rơi.*?[xX](-?\\d+).*?[yY](-?\\d+).*?[zZ](-?\\d+)");

    // Vd: Thính đã bắt đầu rơi!
    private static final Pattern AIRDROP_FALLEN_PATTERN = Pattern
            .compile("(?i)thính đã bắt đầu rơi");

    // Vd: Bạn có đủ nguyên liệu để chế tạo Lumberjack's Axe
    // Also matches: "to craft a Smelter's Pickaxe" (English Hoplite)
    private static final Pattern CRAFTABLE_PATTERN = Pattern.compile("(?i)^(?!.*nhấn vào đây|.*click here).*(?:đủ nguyên liệu để chế tạo |to craft an? )(.*)$");

    // English Hoplite Patterns
    private static final Pattern HOPLITE_CRAFT_PATTERN = Pattern.compile("(?i)(You|\\S+) (?:have|has) crafted (?:the )?(.*?)!");
    private static final Pattern HOPLITE_AIRDROP_PATTERN = Pattern.compile("(?i)supply drop is spawning.*?X=(-?\\d+).*?Z=(-?\\d+)");

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            TeamViewConfig cfg = TeamViewConfig.get();
            if (!cfg.enabled) return;

            String plainText = message.getString().trim();

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
                Matcher m = CRAFTABLE_PATTERN.matcher(plainText);
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
            }
        });
    }
}
