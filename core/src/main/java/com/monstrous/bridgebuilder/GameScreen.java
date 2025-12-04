package com.monstrous.bridgebuilder;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;


public class GameScreen extends ScreenAdapter {

    public Array<Pin> pins;
    public Array<Beam> beams;


    private SpriteBatch spriteBatch;
    private Pin currentPin;


    @Override
    public void show() {
        spriteBatch = new SpriteBatch();



        pins = new Array<>();
        beams = new Array<>();
        populate();
        InputAdapter inputProcessor = new InputAdapter() {

            public boolean touchDragged(int x, int y, int pointer) {
                currentPin.setPosition(x, Gdx.graphics.getHeight() - y);
                return false;
            }

            public boolean touchDown(int x, int y, int pointer, int newParam) {
                if(currentPin != null)
                    return false;
                currentPin = new Pin(x, Gdx.graphics.getHeight() - y);
                pins.add(currentPin);
                return false;
            }

            public boolean touchUp(int x, int y, int pointer, int newParam) {
                currentPin = null;
                return false;
            }

        };
        Gdx.input.setInputProcessor(inputProcessor);

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
//        if(currentPin != null)
//            currentPin.sprite.draw(spriteBatch);
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
