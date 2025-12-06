package com.monstrous.bridgebuilder;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public class Pin {
    private static Texture pinTexture;
    private static int nextId = 1;

    public final int id;
    public boolean isAnchor;
    public final Vector2 position;
    public final Sprite sprite;
    public final float W;
    public final float H;
    public Body body;


    public Pin(float x, float y) {
        this(x, y, false);
    }
    public Pin(float x, float y, boolean isAnchor) {
        id = nextId++;
        this.position = new Vector2(x,y);
        this.isAnchor = isAnchor;

        if(pinTexture == null)
            pinTexture = new Texture("textures/pin.png");
        W = pinTexture.getWidth()/32f;
        H = pinTexture.getHeight()/32f;
        sprite = new Sprite(pinTexture);
        sprite.setOrigin(W/2f, H/2f);;
        sprite.setOriginBasedPosition(x,y);
        sprite.setSize(W, H);
    }

    public void setPosition(float x, float y){
        position.set(x,y);
        sprite.setOriginBasedPosition(x,y);
    }

    public boolean isOver(Vector2 pos){
        if(pos.x < position.x - 0.5*W || pos.x > position.x + 0.5*W)
            return false;
        if(pos.y < position.y - 0.5*H || pos.y > position.y + 0.5* H)
            return false;
        return true;
    }
}
