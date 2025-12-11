package com.monstrous.bridgebuilder;

public enum BuildMaterial {

    DECK (0, 5f, 100f, 20000f),
    WOOD (1, 5f, 10f, 10000f),
    STEEL(2, 8f, 30f, 20000f),
    CABLE (3, 20f, 10f, 20000f);

    public final int index;
    public final float maxLength;
    public final float costPerMeter;
    public final float strength;

    BuildMaterial(int index, float maxLength, float cost, float strength) {
        this.index = index;
        this.maxLength = maxLength;
        this.costPerMeter = cost;
        this.strength = strength;
    }
}
