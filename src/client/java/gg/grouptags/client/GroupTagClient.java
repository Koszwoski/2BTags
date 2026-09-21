package gg.grouptags.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class GroupTagClient implements ClientModInitializer {
    private static final TagService TAGS = new TagService();
    private static final LogoTextureService LOGOS = new LogoTextureService();
    private static final KeyMapping TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
        "key.grouptag.toggle",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G,
        "key.categories.grouptag"
    ));

    private static boolean tagsVisible = true;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.consumeClick()) {
                tagsVisible = !tagsVisible;

                if (client.player != null) {
                    client.player.displayClientMessage(
                        Component.translatable(tagsVisible
                            ? "message.grouptag.enabled"
                            : "message.grouptag.disabled"),
                        true
                    );
                }
            }

            TAGS.tick(client);
        });
    }

    public static List<GroupTag> getTags(UUID playerUuid) {
        return tagsVisible ? TAGS.get(playerUuid) : List.of();
    }

    public static Optional<net.minecraft.resources.ResourceLocation> getLogo(GroupTag tag) {
        return LOGOS.get(tag);
    }
}
