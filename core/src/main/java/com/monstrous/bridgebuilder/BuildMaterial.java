package com.monstrous.bridgebuilder;

public enum BuildMaterial {

    DECK (5f),
    STRUCTURE (8f),
    CABLE (20f);

    public final float maxLength;

    BuildMaterial(float maxLength) {
        this.maxLength = maxLength;
    }
}
