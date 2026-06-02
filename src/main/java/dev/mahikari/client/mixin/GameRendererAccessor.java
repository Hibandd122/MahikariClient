package dev.mahikari.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={GameRenderer.class})
public interface GameRendererAccessor {
    @Invoker(value="getFov")
    public float newgenMahikari$getFov(Camera var1, float var2, boolean var3);
}
