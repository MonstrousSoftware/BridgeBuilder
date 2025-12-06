package com.monstrous.bridgebuilder;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class Beam {
    public static float MAX_LENGTH = 5f;

    public static Texture beamTexture;

    public final Vector2 position1;
    public final Vector2 position2;
    public final Sprite sprite;
    public final float W;
    public final float H;
    public float length;
    public Pin startPin;
    public Pin endPin;


    public Beam(float x, float y, float x2, float y2) {
        this.position1 = new Vector2(x,y);
        this.position2 = new Vector2(x2,y2);

        if(beamTexture == null)
            beamTexture = new Texture("textures/beam.png");

        sprite = new Sprite(beamTexture);
        W = beamTexture.getWidth()/32f;
        H = beamTexture.getHeight()/32f;
        sprite.setOrigin(0, H/2f);
        sprite.setOriginBasedPosition(position1.x, position1.y);
        adaptShape();
    }

    private void adaptShape(){
        float dx = position2.x - position1.x;
        float dy = position2.y - position1.y;
        float angle = (float)Math.atan2(dy, dx);
        float angleDegrees = 180f * angle / (float)Math.PI;

        sprite.setRotation(angleDegrees);

        length = (float)Math.sqrt(dx*dx+dy*dy);
        sprite.setSize(length, H);
    }

    /** adjust position2 so that length does not exceed MAX_LENGTH */
    public void truncateLength(){
        if(length <= MAX_LENGTH)
            return;
        float fraction = MAX_LENGTH/length;
        float dx = position2.x - position1.x;
        float dy = position2.y - position1.y;
        position2.x = position1.x + fraction * dx;
        position2.y = position1.y + fraction * dy;
        adaptShape();
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


}
