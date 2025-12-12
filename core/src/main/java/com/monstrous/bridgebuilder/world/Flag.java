package com.monstrous.bridgebuilder.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Flag implements Json.Serializable {
    private static Texture texture;

    public final Vector2 position;
    public final Sprite sprite;
    public final float W;
    public final float H;
    public Body body;

    public Flag(){
        this.position = new Vector2();
        if(texture == null)
            texture = new Texture("textures/flag.png");
        W = texture.getWidth()/32f;
        H = texture.getHeight()/32f;
        sprite = new Sprite(texture);
        sprite.setOrigin(W/2f, 0f);
        sprite.setSize(W, H);
    }


    public Flag(float x, float y) {
        this();
        setPosition(x,y);
    }

    public void setPosition(float x, float y){
        position.set(x,y);
        sprite.setOriginBasedPosition(x,y);
    }

    @Override
    public void write(Json json) {
        json.writeValue("position", position);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        Vector2 pos = json.readValue("position", Vector2.class, jsonData);
        setPosition(pos.x, pos.y);
    }
}
