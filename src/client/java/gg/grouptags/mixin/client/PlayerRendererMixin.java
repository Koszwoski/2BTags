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
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * In 1.21.1, name tags are declared on EntityRenderer rather than PlayerRenderer.
 * Only player entities receive 2BTags rows.
 */
@Mixin(EntityRenderer.class)
abstract class PlayerRendererMixin {
    @Unique private boolean grouptag$rendering;

    @Shadow @Final protected EntityRenderDispatcher field_4676;

    @Shadow
    protected abstract void method_3926(Entity entity, Component name,
                                          PoseStack poses, MultiBufferSource buffers, int packedLight, float tickDelta);

    @Inject(method = "method_3926", at = @At("TAIL"))
    private void grouptag$render(Entity entity, Component originalName,
                                 PoseStack poses, MultiBufferSource buffers, int packedLight, float tickDelta,
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
                double yOffset = 1.24D + (tags.size() - 1 - index) * 0.22D;

                poses.pushPose();
                poses.translate(0.0D, yOffset, 0.0D);
                method_3926(
                    entity,
                    Component.literal(tag.name()).withColor(tag.color() & 0xFFFFFF),
                    poses,
                    buffers,
                    packedLight,
                    tickDelta
                );
                poses.popPose();

                grouptag$renderLogo(tag, player, yOffset, poses, buffers, packedLight);
            }
        } finally {
            grouptag$rendering = false;
        }
    }

    @Unique
    private void grouptag$renderLogo(GroupTag tag, AbstractClientPlayer player, double yOffset,
                                     PoseStack poses, MultiBufferSource buffers, int packedLight) {
        GroupTagClient.getLogo(tag).ifPresent(texture -> {
            float textWidth = Minecraft.getInstance().font.width(tag.name());
            float iconX = tag.logoAfterName() ? textWidth / 2.0F + 5.0F : -textWidth / 2.0F - 5.0F;
            float halfSize = 4.0F;

            poses.pushPose();
            try {
                poses.translate(0.0D, player.getBbHeight() + 0.5D + yOffset, 0.0D);
                poses.mulPose(field_4676.cameraOrientation());
                poses.scale(0.025F, -0.025F, 0.025F);
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
