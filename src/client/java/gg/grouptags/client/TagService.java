package gg.grouptags.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class TagService {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("2BTags");
    private String lastPlayers = "";
    private String lastResult = "";
    private long nextErrorLogAt;

    TagService() { LOG.info("[2BTags] Multi-group tag service initialized"); }
    private static final String API_URL = "https://api.kosz.dev/v1/tags/lookup?uuids=";
    private static final long REFRESH_INTERVAL_MS = 5_000L;
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private final Map<UUID, List<GroupTag>> tags = new ConcurrentHashMap<>();
    private volatile boolean requestInFlight;
    private long nextRefreshAt;

    List<GroupTag> get(UUID playerUuid) {
        return tags.getOrDefault(playerUuid, List.of());
    }

    void tick(Minecraft client) {
        if (client.level == null) {
            tags.clear();
            lastPlayers = "";
            return;
        }

        if (requestInFlight || System.currentTimeMillis() < nextRefreshAt) {
            return;
        }

        List<UUID> playerUuids = java.util.stream.Stream.concat(
                client.level.players().stream().map(player -> player.getUUID()),
                client.player == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(client.player.getUUID())
            )
            .distinct()
            .limit(100)
            .toList();

        if (playerUuids.isEmpty()) {
            tags.clear();
            nextRefreshAt = System.currentTimeMillis() + REFRESH_INTERVAL_MS;
            return;
        }

        String uuidList = playerUuids.stream()
            .map(UUID::toString)
            .reduce((left, right) -> left + "," + right)
            .orElse("");

        String players = client.level.players().stream()
            .map(player -> player.getName().getString() + "=" + player.getUUID())
            .sorted()
            .collect(java.util.stream.Collectors.joining(", "));

        if (!players.equals(lastPlayers)) {
            LOG.info("[2BTags] Nearby players: {}", players);
            lastPlayers = players;
        }

        var requestLevel = client.level;
        requestInFlight = true;
        nextRefreshAt = System.currentTimeMillis() + REFRESH_INTERVAL_MS;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + URLEncoder.encode(uuidList, StandardCharsets.UTF_8)))
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Lookup HTTP " + response.statusCode());
                }
                return response.body();
            })
            .thenApply(this::parse)
            .thenAccept(result -> client.execute(() -> {
                if (client.level == requestLevel) {
                    tags.clear();
                    tags.putAll(result);

                    String summary = result.toString();
                    if (!summary.equals(lastResult)) {
                        LOG.info("[2BTags] Lookup succeeded: {}", summary);
                        lastResult = summary;
                    }
                }

                requestInFlight = false;
            }))
            .exceptionally(error -> {
                long now = System.currentTimeMillis();
                if (now >= nextErrorLogAt) {
                    LOG.warn("[2BTags] Lookup failed", error);
                    nextErrorLogAt = now + 60_000L;
                }
                requestInFlight = false;
                return null;
            });
    }

    private Map<UUID, List<GroupTag>> parse(String body) {
        Map<UUID, List<GroupTag>> result = new LinkedHashMap<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray entries = root.getAsJsonArray("tags");

        if (entries == null) {
            return result;
        }

        for (JsonElement entry : entries) {
            JsonObject tag = entry.getAsJsonObject();
            UUID uuid = UUID.fromString(tag.get("uuid").getAsString());
            String name = tag.get("displayName").getAsString();
            String color = tag.get("color").getAsString();
            boolean primary = tag.has("isPrimary") && tag.get("isPrimary").getAsBoolean();
            int displayOrder = tag.has("displayOrder") ? tag.get("displayOrder").getAsInt() : 0;

            result.computeIfAbsent(uuid, ignored -> new ArrayList<>()).add(
                new GroupTag(
                    name,
                    parseColor(color),
                    getOptionalString(tag, "logoPath"),
                    getOptionalString(tag, "logoPosition"),
                    primary,
                    displayOrder
                )
            );
        }

        result.replaceAll((uuid, playerTags) -> playerTags.stream()
            .sorted(Comparator
                .comparing(GroupTag::primary).reversed()
                .thenComparingInt(GroupTag::displayOrder)
                .thenComparing(GroupTag::name, String.CASE_INSENSITIVE_ORDER))
            .toList());

        return result;
    }

    private String getOptionalString(JsonObject object, String name) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : "";
    }

    private int parseColor(String color) {
        return 0xFF000000 | Integer.parseInt(color.replace("#", ""), 16);
    }
}
