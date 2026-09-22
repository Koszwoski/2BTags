package gg.grouptags.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

final class LogoTextureService {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("2BTags");
    private static final String ASSET_ORIGIN = "https://assets.grouptags.gg";
    private static final Pattern SAFE_LOGO_PATH = Pattern.compile("^/logos/[a-f0-9-]{36}\\.(?:png|webp)$");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String, ResourceLocation> textures = new ConcurrentHashMap<>();
    private final Set<String> requested = ConcurrentHashMap.newKeySet();

    Optional<ResourceLocation> get(GroupTag tag) {
        if (!tag.hasLogo() || !SAFE_LOGO_PATH.matcher(tag.logoPath()).matches()) return Optional.empty();
        ResourceLocation texture = textures.get(tag.logoPath());
        if (texture != null) return Optional.of(texture);
        if (requested.add(tag.logoPath())) download(tag.logoPath());
        return Optional.empty();
    }

    private void download(String logoPath) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ASSET_ORIGIN + logoPath))
            .timeout(Duration.ofSeconds(10)).GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> {
            if (response.statusCode() != 200) throw new IllegalStateException("Logo HTTP " + response.statusCode());
            if (response.body().length > 4 * 1024 * 1024) throw new IllegalStateException("Logo exceeds 4 MiB");
            return response.body();
        }).thenAccept(bytes -> Minecraft.getInstance().execute(() -> register(logoPath, bytes))).exceptionally(error -> {
            requested.remove(logoPath);
            LOG.warn("[2BTags] Could not download logo {}", logoPath, error);
            return null;
        });
    }

    private void register(String logoPath, byte[] bytes) {
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("grouptag",
                "logos/" + Integer.toUnsignedString(logoPath.hashCode(), 16));
            // Pre-1.21.11 DynamicTexture has no debug-label supplier constructor.
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
            textures.put(logoPath, id);
            LOG.info("[2BTags] Logo loaded: {}", logoPath);
        } catch (Exception error) {
            requested.remove(logoPath);
            LOG.warn("[2BTags] Could not decode logo {}", logoPath, error);
        }
    }
}
