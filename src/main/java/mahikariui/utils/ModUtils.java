/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 */
package mahikariui.utils;

import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;

public class ModUtils {
    private static final Supplier<MinecraftClient> INSTANCE_GETTER = MinecraftClient::getInstance;

    public static Supplier<MinecraftClient> getMinecraftSupplier() {
        return INSTANCE_GETTER;
    }

    public static MinecraftClient getMinecraft() {
        return ModUtils.getMinecraftSupplier().get();
    }
}

