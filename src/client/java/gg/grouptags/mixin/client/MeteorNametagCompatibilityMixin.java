package gg.grouptags.mixin.client;

import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderer.class, priority = 500)
abstract class MeteorNametagCompatibilityMixin {
    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void grouptag$keepAnchorForMeteor(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player player) || GroupTagClient.getTags(player.getUUID()).isEmpty()) {
            return;
        }

        if (grouptag$meteorPlayerNametagsActive()) {
            cir.setReturnValue(Component.empty());
        }
    }

    private static boolean grouptag$meteorPlayerNametagsActive() {
        try {
            Class<?> modulesClass = Class.forName("meteordevelopment.meteorclient.systems.modules.Modules");
            Class<?> nametagsClass = Class.forName("meteordevelopment.meteorclient.systems.modules.render.Nametags");
            Object modules = modulesClass.getMethod("get").invoke(null);
            Object nametags = modulesClass.getMethod("get", Class.class).invoke(modules, nametagsClass);
            return (boolean) nametagsClass.getMethod("playerNametags").invoke(nametags);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
