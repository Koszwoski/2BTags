package gg.grouptags.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.grouptags.client.GroupTag;
import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Minecraft 1.21.1 still renders names directly from the player entity
 * (the render-state API arrived in 1.21.2). Re-use the vanilla name renderer
 * for every tag so distance, text background and accessibility settings match.
 */
@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
    @Unique private boolean grouptag$rendering;

    @Shadow
    protected abstract void renderNameTag(AbstractClientPlayer player, Component name,
                                          PoseStack poses, MultiBufferSource buffers, int packedLight);

    @Inject(method = "renderNameTag", at = @At("TAIL"))
    private void grouptag$render(AbstractClientPlayer player, Component originalName,
                                 PoseStack poses, MultiBufferSource buffers, int packedLight,
                                 CallbackInfo ci) {
        if (grouptag$rendering) {
            return;
        }

        List<GroupTag> tags = GroupTagClient.getTags(player.getUUID());
        if (tags.isEmpty()) {
            return;
        }

        grouptag$rendering = true;
        try {
            for (int index = 0; index < tags.size(); index++) {
                GroupTag tag = tags.get(index);
                double yOffset = 1.24D + (tags.size() - 1 - index) * 0.22D;

                poses.pushPose();
                poses.translate(0.0D, yOffset, 0.0D);
                renderNameTag(
                    player,
                    Component.literal(tag.name()).withColor(tag.color() & 0xFFFFFF),
                    poses,
                    buffers,
                    packedLight
                );
                poses.popPose();
            }
        } finally {
            grouptag$rendering = false;
        }
    }
}
