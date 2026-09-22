package gg.grouptags.client;

public record GroupTag(
    String name,
    int color,
    String logoPath,
    String logoPosition,
    boolean primary,
    int displayOrder
) {
    public boolean hasLogo() {
        return logoPath != null && !logoPath.isBlank();
    }

    public boolean logoAfterName() {
        return "after".equals(logoPosition);
    }
}
