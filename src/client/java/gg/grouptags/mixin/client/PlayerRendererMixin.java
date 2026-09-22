package gg.grouptags.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.grouptags.client.GroupTag;
import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 1.21.1 predates the "render state" rewrite (introduced in 1.21.2), so
 * entities are still rendered directly and renderNameTag still takes the
 * live entity plus a trailing partialTick float that later versions drop.
 *
 * No poseStack.scale() is applied before re-invoking renderNameTag() -
 * vanilla computes its own above-head offset internally, so scaling first
 * would shrink that offset too. The logo, however, is drawn manually (not
 * via a vanilla method), so it does NOT get that above-head offset for
 * free - we have to add entity.getBbHeight() + 0.5 ourselves, matching
 * vanilla's own name-tag anchor height, on top of our extra yOffset.
 */
@Mixin(EntityRenderer.class)
abstract class PlayerRendererMixin {
    @Unique private boolean grouptag$rendering;

    @Shadow
    protected abstract void renderNameTag(Entity entity, Component displayName, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, float partialTick);

    @Inject(method = "renderNameTag", at = @At("TAIL"))
    private void grouptag$appendTags(Entity entity, Component displayName, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, float partialTick,
                                     CallbackInfo ci) {
        if (grouptag$rendering || !(entity instanceof AbstractClientPlayer player)) {
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

                // Primary (first) tag sits highest, closest to the top.
                double yOffset = 0.32D + (tags.size() - 1 - index) * 0.28D;

                poseStack.pushPose();
                poseStack.translate(0.0D, yOffset, 0.0D);

                Component tagComponent = Component.literal(tag.name()).withColor(tag.color() & 0xFFFFFF);
                renderNameTag(entity, tagComponent, poseStack, bufferSource, packedLight, partialTick);
                poseStack.popPose();

                double logoHeight = player.getBbHeight() + 0.5D + yOffset;
                grouptag$renderLogo(tag, poseStack, bufferSource, logoHeight);
            }
        } finally {
            grouptag$rendering = false;
        }
    }

    @Unique
    private void grouptag$renderLogo(GroupTag tag, PoseStack poseStack, MultiBufferSource bufferSource, double height) {
        GroupTagClient.getLogo(tag).ifPresent(texture -> {
            float textWidth = Minecraft.getInstance().font.width(tag.name());
            float iconX = tag.logoAfterName() ? textWidth / 2.0F + 5.0F : -textWidth / 2.0F - 5.0F;
            float halfSize = 4.0F;

            poseStack.pushPose();
            try {
                poseStack.translate(0.0D, height, 0.0D);
                poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
                poseStack.scale(0.025F, -0.025F, 0.025F);
                poseStack.translate(iconX, 4.0F, 0.01F);

                VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(texture));
                PoseStack.Pose pose = poseStack.last();
                consumer.addVertex(pose, -halfSize, -halfSize, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(0xF000F0);
                consumer.addVertex(pose, -halfSize, halfSize, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(0xF000F0);
                consumer.addVertex(pose, halfSize, halfSize, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(0xF000F0);
                consumer.addVertex(pose, halfSize, -halfSize, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(0xF000F0);
            } finally {
                poseStack.popPose();
            }
        });
    }
}
