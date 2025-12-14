package com.monstrous.bridgebuilder.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Flag implements Json.Serializable {
    private static Texture texture;
    private static Texture texture2;
    private static Texture texture3;

    public final Vector2 position;
    public final Array<Sprite> sprites;
    public final float W;
    public final float H;
    public Body body;
    public Body body2;
    public Body body3;
    public RevoluteJoint joint1;
    public RevoluteJoint joint2;

    public Flag(){
        sprites = new Array<>();
        this.position = new Vector2();
        if(texture == null) {
            texture = new Texture("textures/tree1.png");
            texture2 = new Texture("textures/tree2.png");
            texture3 = new Texture("textures/tree3.png");
        }
        W = texture.getWidth()/128f;
        H = texture.getHeight()/128f;
        float scale = 1/128f;
        Sprite sprite = new Sprite(texture);
        sprite.setOrigin(W/2f, 0.5f);
        sprite.setSize(W, H);
        sprites.add(sprite);
        Sprite sprite2 = new Sprite(texture2);
        sprite2.setOrigin(texture2.getWidth()*scale/2f, 0f);
        sprite2.setSize(texture2.getWidth()*scale, texture2.getHeight()*scale);
        sprites.add(sprite2);
        Sprite sprite3 = new Sprite(texture3);
        sprite3.setOrigin(texture3.getWidth()*scale/2f, 0f);
        sprite3.setSize(texture3.getWidth()*scale, texture3.getHeight()*scale);
        sprites.add(sprite3);
    }


    public Flag(float x, float y) {
        this();
        setPosition(x,y);
    }

    public void setPosition(float x, float y){
        position.set(x,y);
        y += 0.5f;
        for(Sprite sprite : sprites) {
            sprite.setOriginBasedPosition(x, y);
            y += 1;
        }
    }

    public void draw(SpriteBatch spriteBatch){
        for(Sprite sprite : sprites)
            sprite.draw(spriteBatch);
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
