package com.monstrous.bridgebuilder.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/** A Beam can be a deck, a wooden beam, a steel beam or a cable */
public class Beam implements Json.Serializable {

    public static Texture[] textures;

    public final Vector2 position1;
    public final Vector2 position2;
    public BuildMaterial material;
    public final Sprite sprite;
    public float W;
    public float H;
    public float length;
    public float angle;
    public Pin startPin;
    public Pin endPin;
    public int startId;
    public int endId;
    public Joint joint;
    public Joint joint2;    // only for deck
    public Body body;       // only for deck
    public Color tint;

    public Beam(){
        this.position1 = new Vector2();
        this.position2 = new Vector2();
        loadTextures();

        material = BuildMaterial.DECK;  // default
        Texture beamTexture = textures[material.index];
        sprite = new Sprite(beamTexture);
        W = beamTexture.getWidth()/16f;
        H = beamTexture.getHeight()/16f;

        sprite.setOrigin(0, H/2f);
        tint = new Color(Color.WHITE);
    }

    /** note: we don't use the texture atlas here because these textures need to be wrapped as repeating
     * textures and that won't work with texture regions.
     */
    private void loadTextures(){
        textures = new Texture[4];

        textures[BuildMaterial.STEEL.index] = new Texture("textures/beam.png");
        textures[BuildMaterial.DECK.index] = new Texture("textures/deck.png");
        textures[BuildMaterial.CABLE.index] = new Texture("textures/cable.png");
        textures[BuildMaterial.WOOD.index] = new Texture("textures/wood.png");

        // have a repeating texture, not a stretched texture
        for(int i = 0; i < 4; i++)
            textures[i].setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }



    public Beam(float x, float y, float x2, float y2) {
        this();
        this.position1.set(x, y);
        this.position2.set(x2, y2);
        sprite.setOriginBasedPosition(position1.x, position1.y);
        adaptShape();
    }

    public void setPositions(Vector2 start, Vector2 end) {
        this.position1.set(start);
        this.position2.set(end);
        sprite.setOriginBasedPosition(start.x, start.y);
        adaptShape();
    }

    private void adaptShape(){
        float dx = position2.x - position1.x;
        float dy = position2.y - position1.y;
        angle = (float)Math.atan2(dy, dx);
        float angleDegrees = 180f * angle / (float)Math.PI;

        sprite.setRotation(angleDegrees);

        length = (float)Math.sqrt(dx*dx+dy*dy);
        sprite.setSize(length, H);
        sprite.setRegion(0,0,length, 1);
    }

    /** max length depending on beam type */
    public float getMaxLength(){
        return material.maxLength;
    }

    public int getCost(){
        return (int)(material.costPerMeter * length);
    }

    /** adjust position2 so that length does not exceed MAX_LENGTH */
    public void truncateLength(){
        float max = getMaxLength();
        if(length <= max)
            return;
        float fraction = max/length;
        float dx = position2.x - position1.x;
        float dy = position2.y - position1.y;
        position2.x = position1.x + fraction * dx;
        position2.y = position1.y + fraction * dy;
        adaptShape();
    }

    public void setColor(Color color){
        tint.set(color);
        sprite.setColor(tint);
    }

    public void setMaterial(BuildMaterial material){
        this.material = material;
        Texture texture = textures[material.index];
        sprite.setTexture( texture );

        W = texture.getWidth()/16f;
        H = texture.getHeight()/16f;
        sprite.setOrigin(0, H/2f);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }

    public void setEndPosition(float x, float y){
        position2.set(x,y);
        adaptShape();
    }

    public void updatePosition(){
        if(endPin == null)  // beam still being created
            return;
        position1.set(startPin.position.x, startPin.position.y);
        position2.set(endPin.position.x, endPin.position.y);
        sprite.setOriginBasedPosition(position1.x, position1.y);
        adaptShape();
    }

    public void setStartPin(Pin pin){
        startPin = pin;
    }

    public void setEndPin(Pin pin){
        endPin = pin;
    }

    public boolean attachedToPin(Pin pin){
        return (pin == startPin || pin == endPin);
    }

    @Override
    public void write(Json json) {
        json.writeValue("startPin", startPin.id);
        json.writeValue("endPin", endPin.id);
        json.writeValue("material", material);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        startId = json.readValue("startPin", Integer.class, jsonData);
        endId = json.readValue("endPin", Integer.class, jsonData);
        BuildMaterial mat = json.readValue("material", BuildMaterial.class, jsonData);
        setMaterial(mat);
    }
}
