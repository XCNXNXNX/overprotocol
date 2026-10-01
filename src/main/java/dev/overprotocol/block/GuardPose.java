package dev.overprotocol.block;

import net.minecraft.util.StringRepresentable;

/** The static poses an honour guard statue can hold. Only the player model's joints are animated. */
public enum GuardPose implements StringRepresentable {
    ATTENTION("attention"),
    SALUTE("salute"),
    PRESENT("present"),
    RAISE("raise");

    public static final GuardPose[] VALUES = values();
    private final String name;

    GuardPose(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The pose a right click with an empty hand moves to. */
    public GuardPose next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public String translationKey() {
        return "overprotocol.pose." + name;
    }
}
