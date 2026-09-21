package gg.grouptags.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.grouptags.client.GroupTag;
import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
    @Unique private final Map<PlayerRenderState, List<GroupTag>> grouptag$tags = new WeakHashMap<>();
    @Unique private boolean grouptag$rendering;

    @Shadow @Final protected EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    protected abstract void renderNameTag(PlayerRenderState state, Component name,
                                          PoseStack poses, MultiBufferSource buffers, int packedLight);

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void grouptag$extract(AbstractClientPlayer player, PlayerRenderState state,
                                  float tickDelta, CallbackInfo ci) {
        List<GroupTag> tags = GroupTagClient.getTags(player.getUUID());
        if (tags.isEmpty()) {
            grouptag$tags.remove(state);
        } else {
            grouptag$tags.put(state, tags);
        }
    }

    @Inject(method = "renderNameTag", at = @At("TAIL"))
    private void grouptag$render(PlayerRenderState state, Component originalName,
                                 PoseStack poses, MultiBufferSource buffers, int packedLight,
                                 CallbackInfo ci) {
        if (grouptag$rendering || state.nameTagAttachment == null) {
            return;
        }

        List<GroupTag> tags = grouptag$tags.get(state);
        if (tags == null || tags.isEmpty()) {
            return;
        }

        Component savedName = state.nameTag;
        Vec3 savedAttachment = state.nameTagAttachment;
        grouptag$rendering = true;

        try {
            for (int index = 0; index < tags.size(); index++) {
                GroupTag tag = tags.get(index);
                double yOffset = 1.24D + (tags.size() - 1 - index) * 0.22D;

                state.nameTag = Component.literal(tag.name()).withColor(tag.color() & 0xFFFFFF);
                state.nameTagAttachment = savedAttachment.add(0.0D, yOffset, 0.0D);

                poses.pushPose();
                poses.scale(0.74F, 0.74F, 0.74F);
                renderNameTag(state, state.nameTag, poses, buffers, packedLight);
                poses.popPose();

                grouptag$renderLogo(tag, savedAttachment, yOffset, poses, buffers, packedLight);
            }
        } finally {
            state.nameTag = savedName;
            state.nameTagAttachment = savedAttachment;
            grouptag$rendering = false;
        }
    }

    @Unique
    private void grouptag$renderLogo(GroupTag tag, Vec3 attachment, double yOffset,
                                     PoseStack poses, MultiBufferSource buffers, int packedLight) {
        GroupTagClient.getLogo(tag).ifPresent(texture -> {
            float textWidth = Minecraft.getInstance().font.width(tag.name());
            float iconX = tag.logoAfterName() ? textWidth / 2.0F + 5.0F : -textWidth / 2.0F - 5.0F;
            float halfSize = 4.0F;

            poses.pushPose();
            try {
                poses.translate(attachment.x, attachment.y + yOffset + 0.5D, attachment.z);
                poses.mulPose(entityRenderDispatcher.cameraOrientation());
                poses.scale(0.025F * 0.74F, -0.025F * 0.74F, 0.025F * 0.74F);
                poses.translate(iconX, 4.0F, 0.01F);

                VertexConsumer vertices = buffers.getBuffer(RenderType.text(texture));
                var matrix = poses.last().pose();
                vertices.addVertex(matrix, -halfSize, -halfSize, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(packedLight);
                vertices.addVertex(matrix, -halfSize, halfSize, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(packedLight);
                vertices.addVertex(matrix, halfSize, halfSize, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(packedLight);
                vertices.addVertex(matrix, halfSize, -halfSize, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(packedLight);
            } finally {
                poses.popPose();
            }
        });
    }
}
