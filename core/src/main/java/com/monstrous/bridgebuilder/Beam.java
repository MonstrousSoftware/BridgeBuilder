package com.monstrous.bridgebuilder;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class Beam {
    public Vector2 position1;
    public Vector2 position2;
    public Sprite sprite;
    public static Texture beamTexture;



    public Beam(float x, float y, float x2, float y2) {
        this.position1 = new Vector2(x,y);
        this.position2 = new Vector2(x2,y2);

        if(beamTexture == null)
            beamTexture = new Texture("textures/beam.png");

        sprite = new Sprite(beamTexture);
        float W = beamTexture.getWidth();
        float H = beamTexture.getHeight();


        sprite.setOriginBasedPosition(position1.x, position1.y);

        float dx = position2.x - position1.x;
        float dy = position2.y - position1.y;
        float angle = (float)Math.atan2(dy, dx);
        float angleDegrees = 180f * angle / (float)Math.PI;

        sprite.setRotation(angleDegrees);

        float len = (float)Math.sqrt(dx*dx+dy*dy);
        sprite.setSize(len+W, H);


    }
}
