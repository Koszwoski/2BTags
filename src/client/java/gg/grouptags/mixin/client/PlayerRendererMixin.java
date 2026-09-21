package gg.grouptags.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.grouptags.client.GroupTag;
import gg.grouptags.client.GroupTagClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(AvatarRenderer.class)
abstract class PlayerRendererMixin {
    @Unique private final Map<AvatarRenderState, List<GroupTag>> grouptag$tags = new WeakHashMap<>();
    @Unique private boolean grouptag$submitting;
    @Unique private final java.util.Set<java.util.UUID> grouptag$seen = new java.util.HashSet<>();
    @Unique private boolean grouptag$reportedSubmit;

    @Shadow
    protected abstract void submitNameTag(AvatarRenderState state, PoseStack poses,
                                          SubmitNodeCollector collector, CameraRenderState camera);

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void grouptag$extract(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        grouptag$tags.remove(state);

        List<GroupTag> tags = GroupTagClient.getTags(player.getUUID());
        if (!tags.isEmpty()) {
            grouptag$tags.put(state, tags);

            if (grouptag$seen.add(player.getUUID())) {
                org.slf4j.LoggerFactory.getLogger("2BTags").info(
                    "[2BTags] Player renderer matched {} to {} group(s)",
                    player.getUUID(),
                    tags.size()
                );
            }
        }
    }

    @Inject(method = "submitNameTag", at = @At("TAIL"))
    private void grouptag$submit(AvatarRenderState state, PoseStack poses,
                                 SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (grouptag$submitting || state.nameTag == null || state.nameTagAttachment == null) {
            return;
        }

        List<GroupTag> tags = grouptag$tags.get(state);
        if (tags == null || tags.isEmpty()) {
            return;
        }

        if (!grouptag$reportedSubmit) {
            org.slf4j.LoggerFactory.getLogger("2BTags").info(
                "[2BTags] Submitting {} group nametag row(s)", tags.size()
            );
            grouptag$reportedSubmit = true;
        }

        Component originalName = state.nameTag;
        Vec3 originalAttachment = state.nameTagAttachment;
        grouptag$submitting = true;

        try {
            for (int index = 0; index < tags.size(); index++) {
                GroupTag tag = tags.get(index);

                // Keep the lowest row clear of the IGN; the primary (first) row is highest.
                // Rows remain compact without touching either each other or the player name.
                double yOffset = 1.24D + (tags.size() - 1 - index) * 0.22D;

                state.nameTag = Component.literal(tag.name()).withColor(tag.color() & 0xFFFFFF);
                state.nameTagAttachment = originalAttachment.add(0.0D, yOffset, 0.0D);

                poses.pushPose();
                poses.scale(0.74F, 0.74F, 0.74F);
                submitNameTag(state, poses, collector, camera);
                poses.popPose();

                grouptag$submitLogo(tag, state, poses, collector, camera);
            }
        } finally {
            state.nameTag = originalName;
            state.nameTagAttachment = originalAttachment;
            grouptag$submitting = false;
        }
    }

    @Unique
    private void grouptag$submitLogo(GroupTag tag, AvatarRenderState state, PoseStack poses,
                                     SubmitNodeCollector collector, CameraRenderState camera) {
        GroupTagClient.getLogo(tag).ifPresent(texture -> {
            float textWidth = net.minecraft.client.Minecraft.getInstance().font.width(tag.name());
            float iconX = tag.logoAfterName() ? textWidth / 2.0F + 5.0F : -textWidth / 2.0F - 5.0F;
            float halfSize = 4.0F;

            poses.pushPose();
            try {
                poses.scale(0.74F, 0.74F, 0.74F);
                poses.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.5D, state.nameTagAttachment.z);
                poses.mulPose(camera.orientation);
                poses.scale(0.025F, -0.025F, 0.025F);
                poses.translate(iconX, 4.0F, 0.01F);

                collector.submitCustomGeometry(
                    poses,
                    net.minecraft.client.renderer.rendertype.RenderTypes.text(texture),
                    (pose, vertices) -> {
                        vertices.addVertex(pose, -halfSize, -halfSize, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(0xF000F0);
                        vertices.addVertex(pose, -halfSize, halfSize, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(0xF000F0);
                        vertices.addVertex(pose, halfSize, halfSize, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(0xF000F0);
                        vertices.addVertex(pose, halfSize, -halfSize, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(0xF000F0);
                    }
                );
            } finally {
                poses.popPose();
            }
        });
    }
}
