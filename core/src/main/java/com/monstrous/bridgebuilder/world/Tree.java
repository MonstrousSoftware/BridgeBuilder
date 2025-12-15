package com.monstrous.bridgebuilder.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Tree implements Json.Serializable {
    private static TextureRegion region;
    private static TextureRegion region2;
    private static TextureRegion region3;

    public final Vector2 position;
    public final Array<Sprite> sprites;
    public float W;
    public float H;
    public Body body;
    public Body body2;
    public Body body3;
    public RevoluteJoint joint1;
    public RevoluteJoint joint2;

    public Tree(){
        sprites = new Array<>();
        this.position = new Vector2();
        initSprites();
    }

    private void initSprites(){
        if(region == null) {
            region = Images.findRegion("tree1");
            region2 = Images.findRegion("tree2");
            region3 = Images.findRegion("tree3");
        }
        W = region.getRegionWidth()/128f;
        H = region.getRegionHeight()/128f;
        float scale = 1/128f;
        Sprite sprite = new Sprite(region);
        sprite.setOrigin(W/2f, 0.5f);
        sprite.setSize(W, H);
        sprites.add(sprite);
        Sprite sprite2 = new Sprite(region2);
        sprite2.setOrigin(region2.getRegionWidth()*scale/2f, 0f);
        sprite2.setSize(region2.getRegionWidth()*scale, region2.getRegionHeight()*scale);
        sprites.add(sprite2);
        Sprite sprite3 = new Sprite(region3);
        sprite3.setOrigin(region3.getRegionWidth()*scale/2f, 0f);
        sprite3.setSize(region3.getRegionWidth()*scale, region3.getRegionHeight()*scale);
        sprites.add(sprite3);
    }


    public Tree(float x, float y) {
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
