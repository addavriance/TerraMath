package me.adda.terramath.config;

import net.minecraft.network.chat.Component;

enum NoiseType {
    PERLIN("Perlin"),
    SIMPLEX("Simplex"),
    BLENDED("Blended"),
    NORMAL("Normal");

    private final Component displayName;

    NoiseType(String name) {
        this.displayName = Component.translatable("terramath.config.noise_type." + name.toLowerCase());
    }

    public Component getDisplayName() {
        return displayName;
    }
}