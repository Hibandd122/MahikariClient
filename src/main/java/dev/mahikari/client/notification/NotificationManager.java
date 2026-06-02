package dev.mahikari.client.notification;

import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.animation.Easings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationManager {
    private static final List<Notification> notifications = new ArrayList<>();
    private static final long DEDUPE_WINDOW_MS = 1500L;

    public static void addNotification(String title, String subtitle, int color, int durationMs) {
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.notificationsEnabled) return;

        long now = System.currentTimeMillis();
        for (Notification existing : notifications) {
            if (existing.title.equals(title) && existing.subtitle.equals(subtitle)
                    && now - existing.startTime < DEDUPE_WINDOW_MS) {
                existing.refresh(now);
                return;
            }
        }

        int adjustedDuration = Math.max(500, Math.round(durationMs * Math.max(0.1f, cfg.notificationDurationMul)));

        int maxStack = Math.max(1, Math.min(8, cfg.notificationMaxStack));
        while (notifications.size() >= maxStack) {
            notifications.remove(0);
        }

        if (cfg.notificationSound) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
            }
        }
        notifications.add(new Notification(title, subtitle, color, adjustedDuration));
    }

    public static void tick() {
        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            Notification n = it.next();
            if (n.isExpired()) {
                it.remove();
            }
        }
    }

    public static void clear() {
        notifications.clear();
    }

    public static boolean removeByTitle(String title) {
        Iterator<Notification> it = notifications.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            Notification n = it.next();
            if (n.title.equals(title)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    public static List<Notification> getActiveNotifications() {
        return notifications;
    }

    public static class Notification {
        private static final int FADE_IN_MS = 250;
        private static final int FADE_OUT_MS = 400;
        private static final int SLIDE_IN_MS = 320;
        private static final int SLIDE_OUT_MS = 280;

        public final String title;
        public final String subtitle;
        public final int color;
        public long startTime;
        public int durationMs;

        public Notification(String title, String subtitle, int color, int durationMs) {
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
            this.startTime = System.currentTimeMillis();
            this.durationMs = durationMs;
        }

        public void refresh(long now) {
            this.startTime = now;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > durationMs;
        }

        public float getAlpha() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < FADE_IN_MS) {
                return Easings.smoothstep(elapsed / (float) FADE_IN_MS);
            }
            int remaining = durationMs - (int) elapsed;
            if (remaining < FADE_OUT_MS) {
                return Easings.smoothstep(Math.max(0.0f, remaining / (float) FADE_OUT_MS));
            }
            return 1.0f;
        }

        public float getSlideProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < SLIDE_IN_MS) {
                return Easings.easeOutExpo(elapsed / (float) SLIDE_IN_MS);
            }
            int remaining = durationMs - (int) elapsed;
            if (remaining < SLIDE_OUT_MS) {
                return Easings.easeOutExpo(Math.max(0.0f, remaining / (float) SLIDE_OUT_MS));
            }
            return 1.0f;
        }

        public float getProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Easings.clamp01(1.0f - ((float) elapsed / durationMs));
        }
    }
}
