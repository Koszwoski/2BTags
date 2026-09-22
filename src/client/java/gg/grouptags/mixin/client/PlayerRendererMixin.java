package gg.grouptags.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.grouptags.client.GroupTag;
import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.21.4 is after the 1.21.2 render-state rewrite but before the 1.21.9
 * SubmitNodeCollector rewrite, so PlayerRenderer already renders from a
 * cached PlayerRenderState, but renderNameTag still draws immediately via
 * PoseStack + MultiBufferSource (no submit/defer step yet).
 *
 * No poseStack.scale() is applied before re-invoking renderNameTag() -
 * vanilla computes its own above-head offset internally. The logo,
 * however, is drawn manually, so it does NOT get that offset for free -
 * we cache the player's bounding-box height in extractRenderState (where
 * the live entity is still available) and add it back in ourselves.
 */
@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
    @Unique private final Map<PlayerRenderState, List<GroupTag>> grouptag$tags = new WeakHashMap<>();
    @Unique private final Map<PlayerRenderState, Float> grouptag$heights = new WeakHashMap<>();
    @Unique private boolean grouptag$rendering;

    @Shadow
    protected abstract void renderNameTag(PlayerRenderState state, Component displayName, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight);

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void grouptag$extract(AbstractClientPlayer player, PlayerRenderState state, float partialTick, CallbackInfo ci) {
        grouptag$tags.remove(state);
        grouptag$heights.remove(state);

        List<GroupTag> tags = GroupTagClient.getTags(player.getUUID());
        if (!tags.isEmpty()) {
            grouptag$tags.put(state, tags);
            grouptag$heights.put(state, player.getBbHeight());
        }
    }

    @Inject(method = "renderNameTag", at = @At("TAIL"))
    private void grouptag$appendTags(PlayerRenderState state, Component displayName, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (grouptag$rendering) {
            return;
        }

        List<GroupTag> tags = grouptag$tags.get(state);
        if (tags == null || tags.isEmpty()) {
            return;
        }

        float bbHeight = grouptag$heights.getOrDefault(state, 1.8F);

        grouptag$rendering = true;
        try {
            for (int index = 0; index < tags.size(); index++) {
                GroupTag tag = tags.get(index);

                // Primary (first) tag sits highest, closest to the top.
                double yOffset = 0.32D + (tags.size() - 1 - index) * 0.28D;

                poseStack.pushPose();
                poseStack.translate(0.0D, yOffset, 0.0D);

                Component tagComponent = Component.literal(tag.name()).withColor(tag.color() & 0xFFFFFF);
                renderNameTag(state, tagComponent, poseStack, bufferSource, packedLight);
                poseStack.popPose();

                double logoHeight = bbHeight + 0.5D + yOffset;
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
