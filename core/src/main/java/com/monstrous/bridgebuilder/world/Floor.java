package com.monstrous.bridgebuilder.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    public Floor() {
        this.position = new Vector2();
        if(texture == null)
            texture = new Texture("textures/ice.png");
        if(texture2 == null)
            texture2 = new Texture("textures/ice2.png");
        W = 50*texture.getWidth()/16f;
        H = texture.getHeight()/16f;
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
        texture2.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
        sprite = new Sprite(texture);
        sprite.setOrigin(W/2f, H);
        sprite.setSize(W, H);
        sprite.setU(-25);
        sprite.setU2(25);

    }

    public void setPosition(float x, float y){
        position.set(x,y);
        sprite.setOriginBasedPosition(x,y);
    }

    public void setShatter(boolean mode){
        if(mode)
            sprite.setTexture(texture2);
        else
            sprite.setTexture(texture);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
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
