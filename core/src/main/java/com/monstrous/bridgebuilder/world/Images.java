package com.monstrous.bridgebuilder.world;


import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Images {
    private static TextureAtlas atlas;

    public Images(TextureAtlas atlas) {
        Images.atlas = atlas;
    }

    public static TextureRegion findRegion(String name){
        return atlas.findRegion(name);
    }
}
