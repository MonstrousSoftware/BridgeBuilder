package com.monstrous.bridgebuilder.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Floor implements Json.Serializable {
    private static Texture texture;
    private static Texture texture2;

    public Vector2 position;
    public Sprite sprite;
    public float W;
    public float H;
    public Body body;
    private TextureRegion region, region2;

    public Floor() {
        this.position = new Vector2();
        initSprites();
    }

    private void initSprites() {
        region = Images.findRegion("ice");
        region2 = Images.findRegion("ice2");
        assert region != null;
        W = 20*region.getRegionWidth()/16f;
        H = region.getRegionHeight()/16f;
        sprite = new Sprite(region);
        sprite.setOrigin(W/2f, H);
        sprite.setSize(W, H);
    }

    public void setPosition(float x, float y){
        position.set(x,y);
        sprite.setOriginBasedPosition(x,y);
    }

    public void setShatter(boolean mode){
        if(mode)
            sprite.setRegion(region2);
        else
            sprite.setRegion(region);
    }

    public void draw(SpriteBatch spriteBatch){
        sprite.draw(spriteBatch);
    }

    @Override
    public void write(Json json) {
        json.writeValue("position", position);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        // note: for teavm, Vector2 has to be added as reflection class
        Vector2 pos = json.readValue("position", Vector2.class, jsonData);
        setPosition(pos.x, pos.y);
    }

}
