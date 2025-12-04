package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;


public class GameScreen extends ScreenAdapter {

    public Array<Pin> pins;
    public Array<Beam> beams;


    private SpriteBatch spriteBatch;

    @Override
    public void show() {
        spriteBatch = new SpriteBatch();



        pins = new Array<>();
        beams = new Array<>();
        populate();
    }

    @Override
    public void render(float delta) {
        // Draw your screen here. "delta" is the time since last render in seconds.
        ScreenUtils.clear(Color.TEAL);

        spriteBatch.begin();

        for(Beam beam : beams){
            beam.sprite.draw(spriteBatch);
        }

        for(Pin pin : pins){
            pin.sprite.draw(spriteBatch);
        }

        spriteBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        // Resize your screen here. The parameters represent the new window size.
    }

    public void populate(){
        Pin anchor1 = new Pin(100, 300);
        pins.add(anchor1);

        Pin anchor2 = new Pin(400, 400);
        pins.add(anchor2);

        Beam beam1 = new Beam(100, 300, 400, 400);
        beams.add(beam1);
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.

    }
}
