package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;


public class GameScreen extends ScreenAdapter {

    public Array<Pin> pins;
    public Array<Beam> beams;


    private SpriteBatch spriteBatch;
    private Pin currentPin;             // is not in pins, non-null during dragging
    private Beam currentBeam;           // is in beams
    private Pin overPin;                // highlighted pin, or null


    @Override
    public void show() {
        spriteBatch = new SpriteBatch();

        pins = new Array<>();
        beams = new Array<>();
        populate();
        InputAdapter inputProcessor = new InputAdapter() {

            @Override
            public boolean mouseMoved(int x, int y) {
                checkOverPin(x, y);
                return false;
            }

            public boolean touchDragged(int x, int y, int pointer) {
                checkOverPin(x, y);
                if(currentPin == null)
                    return false;
                float sy = Gdx.graphics.getHeight() - y;
                currentPin.setPosition(x, sy);
                currentBeam.setEndPosition(x, sy);
                // if the beam gets too long, place a pin and create a new beam
                if(currentBeam.length > Beam.MAX_LENGTH){
                    // we might have overshot the max, so truncate the beam length and get adjusted end position
                    currentBeam.truncateLength();
                    float x2 = currentBeam.position2.x;
                    float y2 = currentBeam.position2.y;
                    currentPin.setPosition(x2, y2);
                    pins.add(currentPin);
                    currentBeam.setEndPin(currentPin);
                    currentBeam = new Beam(x2, y2, x2, y2);
                    currentBeam.setStartPin(currentPin);
                    currentPin = new Pin(x2, y2);
                    beams.add(currentBeam);
                }
                return false;
            }

            public boolean touchDown(int x, int y, int pointer, int button) {
                if(button == Input.Buttons.RIGHT){  // RMB to delete
                    if(overPin != null) {
                        deletePin(overPin);
                        overPin = null;
                    }
                    return false;
                }
                // LMB

                if (currentPin != null) // already dragging
                    return false;
                float sx = x;
                float sy = Gdx.graphics.getHeight() - y;
                float ex = sx;
                float ey = sy;
                Pin startPin;
                if (overPin != null) {    // if we were over a pin, use that as start
                    sx = overPin.position.x;
                    sy = overPin.position.y;
                    startPin = overPin;
                } else {
                    startPin = new Pin(sx, sy);
                    pins.add(startPin);
                }
                currentBeam = new Beam(sx, sy, ex, ey);
                currentBeam.setStartPin(startPin);
                beams.add(currentBeam);
                currentPin = new Pin(ex, ey);

                return false;
            }

            public boolean touchUp(int x, int y, int pointer, int button) {
                if(button == Input.Buttons.RIGHT)
                    return false;
                float sy = Gdx.graphics.getHeight() - y;
                if(currentBeam.startPin.isOver(x,sy)){   // we didn't move from start position, remove beam
                    beams.removeValue(currentBeam, true);
                } else {
                    // if the dragging ends at an existing pin, snap to that instead of making a new pin
                    if (overPin != null) {
                        float sx = overPin.position.x;
                        sy = overPin.position.y;
                        currentBeam.setEndPosition(sx, sy);
                        currentBeam.setEndPin(overPin);
                    } else {
                        pins.add(currentPin);
                        currentBeam.setEndPin(currentPin);
                    }
                }
                currentPin = null;
                currentBeam = null;
                return false;
            }
        };
        Gdx.input.setInputProcessor(inputProcessor);

    }

    /** check if the mouse is over a pin and if so highlight it and assign it to 'overPin' */
    private void checkOverPin(int x, int y) {
        float ty = Gdx.graphics.getHeight() - y;
        if (overPin != null)
            overPin.sprite.setColor(Color.WHITE);
        // check if the mouse is over a pin and if so highlight it
        overPin = null;
        for (Pin pin : pins) {
            if (pin.isOver(x, ty)) {
                overPin = pin;
                overPin.sprite.setColor(Color.RED);
                //System.out.println("highlight pin "+overPin.id);
                break;
            }
        }
    }

    private final Array<Beam> beamsToDelete = new Array<>();

    private void deletePin( Pin pinToDelete ){
        if(pinToDelete.isAnchor)    // cannot delete anchors
            return;
        pins.removeValue(pinToDelete, true);

        // remove all attached beams
        beamsToDelete.clear();
        for(Beam beam : beams){
            if(beam.attachedToPin(pinToDelete))
                beamsToDelete.add(beam);
        }
        beams.removeAll(beamsToDelete, true);

    }

    @Override
    public void render(float delta) {

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
        Pin anchor1 = new Pin(100, 300, true);
        pins.add(anchor1);

        Pin anchor2 = new Pin(650, 400, true);
        pins.add(anchor2);

    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.

    }
}
