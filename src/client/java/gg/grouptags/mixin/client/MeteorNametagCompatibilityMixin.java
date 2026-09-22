package gg.grouptags.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the local player's nametag (and therefore 2BTags) visible while in
 * freecam-style cameras. 1.21.4 is after 1.21.2's render-state rewrite,
 * which added a camera-distance double parameter to shouldShowName.
 */
@Mixin(value = EntityRenderer.class, priority = 500)
abstract class MeteorNametagCompatibilityMixin {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void grouptag$showLocalTagsInFreecam(Entity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == entity && client.getCameraEntity() != entity) {
            cir.setReturnValue(true);
        }
    }
}
