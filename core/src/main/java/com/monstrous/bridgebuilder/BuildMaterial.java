package com.monstrous.bridgebuilder;

public enum BuildMaterial {

    DECK (5f, 100f),
    STRUCTURE (8f, 30f),
    CABLE (20f, 10f);

    public final float maxLength;
    public final float costPerMeter;

    BuildMaterial(float maxLength, float cost) {
        this.maxLength = maxLength;
        this.costPerMeter = cost;
    }
}
