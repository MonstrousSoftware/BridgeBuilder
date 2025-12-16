package com.monstrous.bridgebuilder.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Pin implements Json.Serializable {
    private static int nextId = 1;

    public int id;
    public boolean isAnchor;
    public int anchorDirection;     // 1 left, 2 right, 3 bottom, 4 top
    public Vector2 position;
    public Sprite sprite;
    public Sprite cliffSprite;
    public Sprite pillarSprite;
    public Sprite caneSprite;
    public float W;
    public float H;
    public Body body;
    public float Wc, Hc;
    public float Wp, Hp;
    public float Wk, Hk;

    public Pin(){
        id = nextId++;
        this.position = new Vector2();
        anchorDirection = 0;
        initSprites();
    }

    private void initSprites() {
        TextureRegion region = Images.findRegion("pin");
        W = region.getRegionWidth() / 32f;
        H = region.getRegionHeight() / 32f;
        sprite = new Sprite(region);
        sprite.setOrigin(W/2f, H/2f);
        sprite.setSize(W, H);
        TextureRegion regionCliff = Images.findRegion("cliff");
        Wc = region.getRegionWidth() / 2f;
        Hc = region.getRegionHeight() / 1.5f;
        cliffSprite = new Sprite(regionCliff);
        cliffSprite.setOrigin(0.95f*Wc, 0.64f*Hc);
        cliffSprite.setSize(Wc, Hc);
        TextureRegion regionPillar = Images.findRegion("pillar");
        Wp = region.getRegionWidth() / 16f;
        Hp = regionPillar.getRegionHeight() / 32f;
        pillarSprite = new Sprite(regionPillar);
        pillarSprite.setSize(Wp, Hp);
        pillarSprite.setOrigin(0.5f*Wp, Hp);
        TextureRegion regionCane = Images.findRegion("candycane");
        Wk = regionCane.getRegionWidth() / 32f;
        Hk = regionCane.getRegionHeight() / 40f;
        caneSprite = new Sprite(regionCane);
        caneSprite.setSize(Wk, Hk);
        caneSprite.setOrigin(0.8f*Wk, 0.85f*Hk);
    }

    public Pin(float x, float y) {
        this(x, y, false, 0);
    }

    public Pin(float x, float y, boolean isAnchor, int direction) {
        this();
        setPosition(x,y);
        this.isAnchor = isAnchor;
        this.anchorDirection = direction;

    }

    public void setPosition(float x, float y){
        position.set(x,y);
        pillarSprite.setOriginBasedPosition(x,y);
        cliffSprite.setOriginBasedPosition(x,y);
        caneSprite.setOriginBasedPosition(x,y);
        sprite.setOriginBasedPosition(x,y);

    }

    public boolean isOver(Vector2 pos){
        if(pos.x < position.x - W || pos.x > position.x + W)
            return false;
        if(pos.y < position.y - H || pos.y > position.y + H)
            return false;
        return true;
    }

    public void draw(SpriteBatch spriteBatch){
        if(isAnchor && anchorDirection == 1)
            cliffSprite.draw(spriteBatch);
        if(isAnchor && anchorDirection == 2) {
            cliffSprite.setScale(-1f, 1f);
            //cliffSprite.setOrigin(0.05f*Wc, 0.64f*Hc);
            cliffSprite.draw(spriteBatch);
        }
        if(isAnchor && anchorDirection == 3)
            pillarSprite.draw(spriteBatch);
        if(isAnchor && anchorDirection == 4)
            caneSprite.draw(spriteBatch);
        if(isAnchor && anchorDirection == 5) {
            caneSprite.setScale(-1f, 1f);
            caneSprite.draw(spriteBatch);
        }
        sprite.draw(spriteBatch);

            //spriteBatch.draw(cliffTexture, 0,0);
    }

    public int getCost(){
        return (isAnchor ? 0 : 10);
    }



    @Override
    public void write(Json json) {
        json.writeValue("id", id);
        json.writeValue("isAnchor", isAnchor);
        json.writeValue("anchorDirection", anchorDirection);
        json.writeValue("position", position);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        id = json.readValue("id", Integer.class, jsonData);
        isAnchor = json.readValue("isAnchor", Boolean.class, jsonData);
        anchorDirection = json.readValue("anchorDirection", Integer.class, jsonData);

        // note: for teavm, Vector2 has to be added as reflection class
        Vector2 pos = json.readValue("position", Vector2.class, jsonData);
        //System.out.println("read pin " + id+ " pos: "+pos);


        setPosition(pos.x, pos.y);
    }
}
