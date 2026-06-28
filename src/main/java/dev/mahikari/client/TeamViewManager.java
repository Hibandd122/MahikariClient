package dev.mahikari.client;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamViewManager {
    private final Map<String, TeammateData> teammates = new ConcurrentHashMap<String, TeammateData>();
    private volatile String currentMode = "";

    public String getCurrentMode() {
        return this.currentMode;
    }

    public void setCurrentMode(String mode) {
        this.currentMode = mode != null ? mode : "";
    }

    public void addTeammate(String name) {
        String cleanName = cleanDisplayName(name);
        if (cleanName.isEmpty()) return;
        this.teammates.putIfAbsent(getNameKey(cleanName), new TeammateData(cleanName));
    }

    public void removeTeammate(String name) {
        this.teammates.remove(getNameKey(name));
    }

    public void clearAll() {
        this.teammates.clear();
    }

    public boolean isTeammate(String name) {
        return this.teammates.containsKey(getNameKey(name));
    }

    public TeammateData findByName(String name) {
        return this.teammates.get(getNameKey(name));
    }

    public TeammateData findByUuid(java.util.UUID uuid) {
        if (uuid == null) return null;
        for (TeammateData data : this.teammates.values()) {
            if (uuid.equals(data.getUuid())) {
                return data;
            }
        }
        return null;
    }

    public Collection<TeammateData> getAll() {
        return this.teammates.values();
    }

    public void updatePosition(String name, String world, double x, double y, double z, String biome, String role) {
        String cleanName = cleanDisplayName(name);
        if (cleanName.isEmpty()) return;
        String key = getNameKey(cleanName);
        TeammateData data = this.teammates.get(key);
        if (data != null) {
            data.updateFromServer(world, x, y, z, biome, role);
        } else {
            TeammateData auto = new TeammateData(cleanName);
            auto.updateFromServer(world, x, y, z, biome, role);
            this.teammates.put(key, auto);
        }
    }

    public void updateApolloPosition(String name, UUID uuid, java.awt.Color color, String world, double x, double y, double z) {
        String cleanName = cleanDisplayName(name);
        if (cleanName.isEmpty()) return;

        TeammateData data = uuid != null ? findByUuid(uuid) : null;
        String key = getNameKey(data != null ? data.getName() : cleanName);
        if (data == null) {
            data = this.teammates.get(key);
        }
        if (data == null) {
            data = new TeammateData(cleanName);
            key = getNameKey(cleanName);
            this.teammates.put(key, data);
        }
        data.updateFromServer(world, x, y, z, "", "apollo");
        data.updateApolloData(uuid, color);
    }

    public void setRole(String name, String role) {
        TeammateData data = this.teammates.get(getNameKey(name));
        if (data != null) {
            data.role = role != null ? role : "";
        }
    }

    public void setOffline(String name) {
        TeammateData data = this.teammates.get(getNameKey(name));
        if (data != null) {
            data.markOffline();
        }
    }

    public void updateHealth(String name, float cur, float max, float absorption) {
        String cleanName = cleanDisplayName(name);
        if (cleanName.isEmpty()) return;
        String key = getNameKey(cleanName);
        TeammateData data = this.teammates.get(key);
        if (data == null) {
            data = new TeammateData(cleanName);
            this.teammates.put(key, data);
        }
        data.updateHealth(cur, max, absorption);
    }

    public void updateRemoteEffects(String name, List<RemoteEffect> effects) {
        String cleanName = cleanDisplayName(name);
        if (cleanName.isEmpty()) return;
        String key = getNameKey(cleanName);
        TeammateData data = this.teammates.get(key);
        if (data == null) {
            data = new TeammateData(cleanName);
            this.teammates.put(key, data);
        }
        data.updateRemoteEffects(effects);
    }

    public static String getNameKey(String name) {
        String cleanName = cleanDisplayName(name);
        String identityName = extractUsernameToken(cleanName);
        StringBuilder key = new StringBuilder(identityName.length());
        for (int i = 0; i < identityName.length(); i++) {
            char c = identityName.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
                key.append(Character.toLowerCase(c));
            }
        }
        return key.toString();
    }

    public static String cleanDisplayName(String name) {
        if (name == null) return "";
        String trimmed = name.replace("\"", "").trim();
        StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '\u00a7' && i + 1 < trimmed.length()) {
                i++;
                continue;
            }
            if (!Character.isISOControl(c)) {
                out.append(c);
            }
        }
        return out.toString().trim();
    }

    private static String extractUsernameToken(String cleanName) {
        String best = "";
        int start = -1;
        for (int i = 0; i <= cleanName.length(); i++) {
            char c = i < cleanName.length() ? cleanName.charAt(i) : ' ';
            boolean usernameChar = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
            if (usernameChar) {
                if (start < 0) start = i;
            } else if (start >= 0) {
                String token = cleanName.substring(start, i);
                if (token.length() >= 3 && token.length() <= 16) {
                    best = token;
                }
                start = -1;
            }
        }
        return best.isEmpty() ? cleanName : best;
    }

    public static class TeammateData {
        private final String name;
        private volatile String world = "";
        private volatile String biome = "";
        private volatile String role = "";
        private volatile double serverX;
        private volatile double serverY;
        private volatile double serverZ;
        private volatile long lastServerUpdate;
        private volatile boolean online = true;
        // Server-sourced health (from HEALTH| packets â€” authoritative for maxHealth)
        private volatile float serverHealth = -1.0f;
        private volatile float serverMaxHealth = -1.0f;
        private volatile float serverAbsorption = 0.0f;
        private volatile long lastServerHealthMs;
        // Local entity-sourced health (from data tracker â€” real-time for nearby players)
        private volatile float localHealth = -1.0f;
        private volatile float localMaxHealth = -1.0f;
        private volatile float localAbsorption = 0.0f;
        private volatile long lastLocalHealthMs;
        // Resolved (merged) values used by renderer
        private volatile float health = 20.0f;
        private volatile float maxHealth = 20.0f;
        private volatile float absorption = 0.0f;
        private float displayHealth = 20.0f;
        private float trailHealth = 20.0f;
        private float displayAbs = 0.0f;
        private float trailAbs = 0.0f;
        private long lastHealthDisplayNano;
        private volatile float prevHpForAnim = -1.0f;
        private volatile long deathAnimStartMs = 0L;
        private volatile long lastDamageTimeMs = 0L;
        private volatile List<RemoteEffect> remoteEffects = Collections.emptyList();
        private volatile long lastRemoteEffectsMs = 0L;
        private double displayX;
        private double displayY;
        private double displayZ;
        private boolean displayInitialized;
        private long lastDisplayNano;
        private volatile UUID uuid;
        private volatile java.awt.Color apolloColor;

        public TeammateData(String name) {
            this.name = name;
        }

        /**
         * Update from server HEALTH| packet â€” authoritative source.
         * Server maxHealth is always trusted over local entity maxHealth.
         */
        public void updateHealth(float cur, float max, float absorption) {
            if (max <= 0.0f) {
                max = 20.0f;
            }
            float newSrv = Math.max(0.0f, cur);
            float newAbs = Math.max(0.0f, absorption);
            if (Math.abs(newSrv - this.serverHealth) > 0.01f || Math.abs(newAbs - this.serverAbsorption) > 0.01f) {
                this.lastServerHealthMs = System.currentTimeMillis();
            }
            this.serverHealth = newSrv;
            this.serverMaxHealth = max;
            this.serverAbsorption = newAbs;
            resolveHealth();
        }

        /**
         * Update from local entity (data tracker) â€” real-time for nearby visible players.
         * Provides instant damage/heal feedback but maxHealth may be inaccurate.
         */
        public void updateLocalHealth(float cur, float max, float absorption) {
            float newLocal = Math.max(0.0f, cur);
            float newAbs = Math.max(0.0f, absorption);
            if (Math.abs(newLocal - this.localHealth) > 0.01f || Math.abs(newAbs - this.localAbsorption) > 0.01f) {
                this.lastLocalHealthMs = System.currentTimeMillis();
            }
            this.localHealth = newLocal;
            this.localMaxHealth = max > 0.0f ? max : 20.0f;
            this.localAbsorption = newAbs;
            resolveHealth();
        }

        /**
         * Smart merge: pick the best value from each source.
         * - maxHealth: always prefer server (vanilla client doesn't sync attributes reliably)
         * - currentHealth/absorption: use whichever source updated most recently
         */
        private void resolveHealth() {
            // Snapshot all volatile fields to local variables for consistent resolution
            long srvMs = this.lastServerHealthMs;
            long locMs = this.lastLocalHealthMs;
            float srvHp = this.serverHealth;
            float srvMax = this.serverMaxHealth;
            float srvAbs = this.serverAbsorption;
            float locHp = this.localHealth;
            float locMax = this.localMaxHealth;
            float locAbs = this.localAbsorption;

            float oldHealth = this.health;
            boolean hasServer = srvMs > 0L;
            boolean hasLocal = locMs > 0L;

            // maxHealth: server is authoritative, local is fallback
            if (hasServer && srvMax > 0.0f) {
                this.maxHealth = srvMax;
            } else if (hasLocal) {
                this.maxHealth = locMax;
            }

            // currentHealth + absorption: prefer the most recent source
            if (hasServer && hasLocal) {
                if (locMs >= srvMs) {
                    this.health = locHp;
                    this.absorption = locAbs;
                } else {
                    this.health = srvHp;
                    this.absorption = srvAbs;
                }
            } else if (hasServer) {
                this.health = srvHp;
                this.absorption = srvAbs;
            } else if (hasLocal) {
                this.health = locHp;
                this.absorption = locAbs;
            }
            
            if (oldHealth - this.health > 0.2f) {
                this.lastDamageTimeMs = System.currentTimeMillis();
            }
        }

        public boolean hasRecentHealth() {
            long now = System.currentTimeMillis();
            boolean recentServer = this.lastServerHealthMs > 0L && now - this.lastServerHealthMs < 8000L;
            boolean recentLocal = this.lastLocalHealthMs > 0L && now - this.lastLocalHealthMs < 3000L;
            return recentServer || recentLocal;
        }

        public boolean hasRecentServerHealth() {
            return this.lastServerHealthMs > 0L && System.currentTimeMillis() - this.lastServerHealthMs < 8000L;
        }

        public long getLastServerHealthMs() {
            return this.lastServerHealthMs;
        }

        public long getLastLocalHealthMs() {
            return this.lastLocalHealthMs;
        }

        public float getHealth() {
            return this.health;
        }

        public float getMaxHealth() {
            return this.maxHealth;
        }
        
        public float getAbsorption() {
            return this.absorption;
        }

        public float getDisplayHealth() {
            return this.displayHealth;
        }

        public float getTrailHealth() {
            return this.trailHealth;
        }

        public float getDisplayAbs() {
            return this.displayAbs;
        }

        public float getTrailAbs() {
            return this.trailAbs;
        }

        public void smoothHealthToward(float targetHp, float targetAbs) {
            long now = System.nanoTime();
            if (this.lastHealthDisplayNano == 0L) {
                this.displayHealth = targetHp;
                this.trailHealth = targetHp;
                this.displayAbs = targetAbs;
                this.trailAbs = targetAbs;
                this.lastHealthDisplayNano = now;
                return;
            }
            float dt = (float) ((now - this.lastHealthDisplayNano) / 1.0E9);
            this.lastHealthDisplayNano = now;
            if (dt <= 0.0f || dt > 1.0f) {
                this.displayHealth = targetHp;
                this.trailHealth = targetHp;
                this.displayAbs = targetAbs;
                this.trailAbs = targetAbs;
                return;
            }
            
            // HP Smoothing
            this.displayHealth = dev.mahikari.client.animation.Easings.expSmooth(this.displayHealth, targetHp, 0.12f, dt);
            if (targetHp > this.trailHealth) {
                this.trailHealth = targetHp;
            } else {
                this.trailHealth = dev.mahikari.client.animation.Easings.expSmooth(this.trailHealth, targetHp, 0.6f, dt);
            }

            // Absorption Smoothing
            this.displayAbs = dev.mahikari.client.animation.Easings.expSmooth(this.displayAbs, targetAbs, 0.12f, dt);
            if (targetAbs > this.trailAbs) {
                this.trailAbs = targetAbs;
            } else {
                this.trailAbs = dev.mahikari.client.animation.Easings.expSmooth(this.trailAbs, targetAbs, 0.6f, dt);
            }
        }

        public void updateFromServer(String world, double x, double y, double z, String biome, String role) {
            this.world = world;
            this.serverX = x;
            this.serverY = y;
            this.serverZ = z;
            this.biome = biome != null ? biome : "";
            this.role = role != null ? role : "";
            this.lastServerUpdate = System.currentTimeMillis();
            this.online = true;
            if (!this.displayInitialized) {
                this.displayX = x;
                this.displayY = y;
                this.displayZ = z;
                this.displayInitialized = true;
                this.lastDisplayNano = System.nanoTime();
            }
        }

        public void markOffline() {
            this.online = false;
        }

        public boolean hasRecentServerData() {
            if (this.name.startsWith("TestPlayer")) return true;
            return this.online && this.lastServerUpdate > 0L && System.currentTimeMillis() - this.lastServerUpdate < 5000L;
        }

        public void updateApolloData(UUID uuid, java.awt.Color color) {
            if (uuid != null) {
                this.uuid = uuid;
            }
            this.apolloColor = color;
        }

        public void updateUuid(UUID uuid) {
            if (uuid != null) {
                this.uuid = uuid;
            }
        }

        public void updateApolloColor(java.awt.Color color) {
            if (color != null) {
                this.apolloColor = color;
            }
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public java.awt.Color getApolloColor() {
            return this.apolloColor;
        }

        public void smoothToward(double targetX, double targetY, double targetZ) {
            if (!this.displayInitialized) {
                this.displayX = targetX;
                this.displayY = targetY;
                this.displayZ = targetZ;
                this.displayInitialized = true;
                this.lastDisplayNano = System.nanoTime();
                return;
            }
            long now = System.nanoTime();
            double dt = (double)(now - this.lastDisplayNano) / 1.0E9;
            this.lastDisplayNano = now;
            if (dt <= 0.0 || dt > 1.0) {
                this.displayX = targetX;
                this.displayY = targetY;
                this.displayZ = targetZ;
                return;
            }
            double factor = 1.0 - Math.exp(-dt / 0.04);
            this.displayX += (targetX - this.displayX) * factor;
            this.displayY += (targetY - this.displayY) * factor;
            this.displayZ += (targetZ - this.displayZ) * factor;
        }

        public void snapDisplay(double x, double y, double z) {
            this.displayX = x;
            this.displayY = y;
            this.displayZ = z;
            this.displayInitialized = true;
            this.lastDisplayNano = System.nanoTime();
        }

        public void checkDeathTransition() {
            float now = this.health;
            if (this.prevHpForAnim > 0.0f && now <= 0.0f && this.deathAnimStartMs == 0L) {
                this.deathAnimStartMs = System.currentTimeMillis();
            }
            if (now > 0.0f) {
                this.deathAnimStartMs = 0L;
            }
            this.prevHpForAnim = now;
        }

        public long getDeathAnimStartMs() {
            return this.deathAnimStartMs;
        }

        public long getLastDamageTimeMs() {
            return this.lastDamageTimeMs;
        }

        public void clearDeathAnim() {
            this.deathAnimStartMs = 0L;
        }

        public void updateRemoteEffects(List<RemoteEffect> effects) {
            this.remoteEffects = effects != null ? effects : Collections.emptyList();
            this.lastRemoteEffectsMs = System.currentTimeMillis();
        }

        public List<RemoteEffect> getRemoteEffects() {
            if (this.lastRemoteEffectsMs > 0L && System.currentTimeMillis() - this.lastRemoteEffectsMs > 20000L) {
                return Collections.emptyList();
            }
            return this.remoteEffects;
        }

        public int getEstimatedRemainingTicks(RemoteEffect eff) {
            long elapsedMs = System.currentTimeMillis() - this.lastRemoteEffectsMs;
            int elapsedTicks = (int)(elapsedMs / 50L);
            return Math.max(0, eff.duration - elapsedTicks);
        }

        public String getName() {
            return this.name;
        }

        public String getWorld() {
            return this.world;
        }

        public String getBiome() {
            return this.biome;
        }

        public String getRole() {
            String normalizedRole = normalizeRole(this.role);
            if ("apollo".equals(normalizedRole) && this.apolloColor != null) {
                int r = this.apolloColor.getRed();
                int g = this.apolloColor.getGreen();
                int b = this.apolloColor.getBlue();

                // Cách nhận diện màu nhanh:
                // Xanh biển/Cyan (Party): Blue cao hơn Red và lớn hơn hoặc bằng Green
                if (b > r && b >= g) return "party";
                
                // Xanh lá (Team): Green trội nhất
                if (g > r && g > b) return "team";
                
                // Vàng/Cam (King): Red và Green đều cao, Blue thấp
                if (r > b && g > b) return "king";
            }
            return normalizedRole;
        }

        private static String normalizeRole(String role) {
            if (role == null) return "";
            return switch (role.trim().toLowerCase(Locale.ROOT)) {
                case "vip", "premium", "donator", "donor", "rank_vip" -> "vip";
                case "king", "leader", "owner" -> "king";
                case "party", "ally", "friend" -> "party";
                case "team", "member", "teammate" -> "team";
                case "apollo" -> "apollo";
                default -> role.trim().toLowerCase(Locale.ROOT);
            };
        }

        public double getServerX() {
            return this.serverX;
        }

        public double getServerY() {
            return this.serverY;
        }

        public double getServerZ() {
            return this.serverZ;
        }

        public double getDisplayX() {
            return this.displayX;
        }

        public double getDisplayY() {
            return this.displayY;
        }

        public double getDisplayZ() {
            return this.displayZ;
        }

        public boolean isOnline() {
            return this.online;
        }
    }

    public static final class RemoteEffect {
        public final String effectId;
        public final int duration;
        public final int amplifier;

        public RemoteEffect(String effectId, int duration, int amplifier) {
            this.effectId = effectId;
            this.duration = duration;
            this.amplifier = amplifier;
        }
    }
}
