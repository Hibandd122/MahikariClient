/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.world.World
 *  net.minecraft.client.MinecraftClient
 */
package mahikariui.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.client.MinecraftClient;

public class WorldUtils {
    public static PlayerEntity getPlayer(MinecraftClient client) {
        return client != null ? client.player : null;
    }

    public static World getWorld(MinecraftClient client) {
        return client != null ? client.world : null;
    }
}

