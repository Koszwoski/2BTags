package gg.grouptags.mixin.client;

import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps a nametag anchor for 2BTags whenever Meteor hides vanilla labels,
 * and makes the local player's groups visible above their body in freecam.
 */
@Mixin(value = EntityRenderer.class, priority = 500)
abstract class MeteorNametagCompatibilityMixin {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void grouptag$showLocalTagsInFreecam(Entity entity, double distance,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (grouptag$isLocalPlayerInFreecam(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void grouptag$keepAnchor(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Avatar player) || GroupTagClient.getTags(player.getUUID()).isEmpty()) {
            return;
        }

        if (grouptag$isLocalPlayerInFreecam(player) || grouptag$meteorPlayerNametagsActive()) {
            cir.setReturnValue(Component.empty());
        }
    }

    private static boolean grouptag$isLocalPlayerInFreecam(Entity entity) {
        Minecraft client = Minecraft.getInstance();
        return client.player == entity && client.getCameraEntity() != entity;
    }

    private static boolean grouptag$meteorPlayerNametagsActive() {
        try {
            Class<?> modulesClass = Class.forName("meteordevelopment.meteorclient.systems.modules.Modules");
            Class<?> nametagsClass = Class.forName(
                "meteordevelopment.meteorclient.systems.modules.render.Nametags"
            );
            Object modules = modulesClass.getMethod("get").invoke(null);
            Object nametags = modulesClass.getMethod("get", Class.class).invoke(modules, nametagsClass);
            return (boolean) nametagsClass.getMethod("playerNametags").invoke(nametags);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
