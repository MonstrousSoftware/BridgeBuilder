package com.monstrous.bridgebuilder;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class Pin {
    private static Texture pinTexture;

    public Vector2 position;
    public Sprite sprite;

    public Pin(float x, float y) {
        this.position = new Vector2(x,y);

        if(pinTexture == null)
            pinTexture = new Texture("textures/pin.png");
        sprite = new Sprite(pinTexture);
        sprite.setOriginCenter();
        sprite.setOriginBasedPosition(x,y);
        sprite.setSize(pinTexture.getWidth(), pinTexture.getHeight());
    }
}
