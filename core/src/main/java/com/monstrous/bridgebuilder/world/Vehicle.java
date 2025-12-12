package com.monstrous.bridgebuilder.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Vehicle implements Json.Serializable {
    private static Texture pinTexture;

    public final Vector2 position;
    public float angle;
    public final Sprite sprite;
    public final float W;
    public final float H;
    public Body body;

    public Vehicle(){
        this.position = new Vector2();
        if(pinTexture == null)
            pinTexture = new Texture("textures/vehicle.png");
        W = pinTexture.getWidth()/16f;
        H = pinTexture.getHeight()/16f;
        sprite = new Sprite(pinTexture);
        sprite.setOrigin(W/2f, H/2f);;
        sprite.setSize(W, H);
    }


    public Vehicle(float x, float y) {
        this();
        setPosition(x,y);
    }

    public void setPosition(float x, float y){
        position.set(x,y);
        sprite.setOriginBasedPosition(x,y);
    }

    public void setRotation(float angle){
        this.angle = angle;
        sprite.setRotation(angle*180f/(float)Math.PI);
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
