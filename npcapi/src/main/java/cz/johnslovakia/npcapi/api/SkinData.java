package cz.johnslovakia.npcapi.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents Minecraft player skin data (texture + signature from Mojang API).
 */
public final class SkinData {

    /** Default Steve skin */
    public static final SkinData STEVE = new SkinData(
            "ewogICJ0aW1lc3RhbXAiIDogMTcwMDAwMDAwMDAwMCwKICAicHJvZmlsZUlkIiA6ICI4NjY3YmE3MWI4NWE0MDA0YWY1NDZhNzdiZmFkYmJlNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdGV2ZSIsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82MGE1YmQwMTZiM2M5YTFiOTI3MmU0OTI5ZTMwODI3YTY3YmU0ZWJiMjE5ZDJlYzE1Y2Q5MDMwYWQ3NTNlYjQiCiAgICB9CiAgfQp9",
            null
    );

    /** Default Alex skin */
    public static final SkinData ALEX = new SkinData(
            "ewogICJ0aW1lc3RhbXAiIDogMTcwMDAwMDAwMDAwMCwKICAicHJvZmlsZUlkIiA6ICI3MTI1OTlhMzNhMWQ0MjE2YTY3MjI2OTg4ZTMwMGEyNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJBbGV4IiwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2VmZTNkOTQ2ZWY4MTYxNzZlZWE3NTQxZDQ3ZWYwNTdjNzIxNTkzOGFmNzk2Y2UzYTY5YjRiMzY2ZjI0YzMifQogIH0KfQ==",
            null
    );

    private final String value;
    private final String signature;

    public SkinData(@NotNull String value, @Nullable String signature) {
        this.value = value;
        this.signature = signature;
    }

    /**
     * Base64-encoded texture data.
     */
    @NotNull
    public String getValue() {
        return value;
    }

    /**
     * Mojang signature for the texture (may be null for unsigned skins).
     */
    @Nullable
    public String getSignature() {
        return signature;
    }

    public boolean isSigned() {
        return signature != null && !signature.isEmpty();
    }

    @Override
    public String toString() {
        return "SkinData{signed=" + isSigned() + "}";
    }
}
