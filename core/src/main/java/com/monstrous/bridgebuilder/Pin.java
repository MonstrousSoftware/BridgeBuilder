package com.monstrous.bridgebuilder;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Pin implements Json.Serializable {
    private static Texture pinTexture;
    private static int nextId = 1;

    public int id;
    public boolean isAnchor;
    public final Vector2 position;
    public final Sprite sprite;
    public final float W;
    public final float H;
    public Body body;

    public Pin(){
        id = nextId++;
        this.position = new Vector2();
        if(pinTexture == null)
            pinTexture = new Texture("textures/pin.png");
        W = pinTexture.getWidth()/32f;
        H = pinTexture.getHeight()/32f;
        sprite = new Sprite(pinTexture);
        sprite.setOrigin(W/2f, H/2f);;
        sprite.setSize(W, H);
    }

    public Pin(float x, float y) {
        this(x, y, false);
    }

    public Pin(float x, float y, boolean isAnchor) {
        this();
        setPosition(x,y);
        this.isAnchor = isAnchor;
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

    @Override
    public void write(Json json) {
        json.writeValue("id", id);
        json.writeValue("isAnchor", isAnchor);
        json.writeValue("position", position);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        id = json.readValue("id", Integer.class, jsonData);
        isAnchor = json.readValue("isAnchor", Boolean.class, jsonData);
        Vector2 pos = json.readValue("position", Vector2.class, jsonData);
        setPosition(pos.x, pos.y);
    }
}
