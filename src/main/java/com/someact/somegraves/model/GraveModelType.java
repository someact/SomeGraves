package com.someact.somegraves.model;

/**
 * Visual model styles supported by SomeGraves.
 */
public enum GraveModelType {
    PLAYER_HEAD,
    CHEST,
    BARREL,
    ENDER_CHEST,
    ITEM_DISPLAY,
    BLOCK_DISPLAY,
    ARMOR_STAND;

    public static GraveModelType fromString(String name, GraveModelType fallback) {
        if (name == null) return fallback;
        try {
            return valueOf(name.toUpperCase().trim());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
