package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;


public class GameScreen extends ScreenAdapter {

    public Array<Pin> pins;
    public Array<Beam> beams;

    public Physics physics;
    private OrthographicCamera camera;


    private SpriteBatch spriteBatch;
    private Pin currentPin;             // is not in pins, non-null during dragging
    private Beam currentBeam;           // is in beams
    private Pin overPin;                // highlighted pin, or null
    private Vector2 worldPos = new Vector2();
    private Vector2 startPos = new Vector2();
    private Vector2 correctedPos = new Vector2();
    public boolean runPhysics = false;


    @Override
    public void show() {
        spriteBatch = new SpriteBatch();
        physics = new Physics();

        // for debug renderer
        camera = new OrthographicCamera(Gdx.graphics.getWidth()/32f, Gdx.graphics.getHeight()/32f);

        pins = new Array<>();
        beams = new Array<>();
        populate();
        InputAdapter inputProcessor = new InputAdapter() {

            @Override
            public boolean mouseMoved(int x, int y) {
                screenToWorldUnits(x,y, worldPos);
                checkOverPin(worldPos);
                return false;
            }

            public boolean touchDragged(int x, int y, int pointer) {
                screenToWorldUnits(x,y, worldPos);
                checkOverPin(worldPos);
                if(currentPin == null)
                    return false;
                //float sy = Gdx.graphics.getHeight() - y;
                currentPin.setPosition(worldPos.x, worldPos.y);
                currentBeam.setEndPosition(worldPos.x, worldPos.y);
                // if the beam gets too long, place a pin and create a new beam
                if(currentBeam.length > Beam.MAX_LENGTH){
                    // we might have overshot the max, so truncate the beam length and get adjusted end position
                    currentBeam.truncateLength();
                    correctedPos.set(currentBeam.position2.x, currentBeam.position2.y);
//                    float x2 = currentBeam.position2.x;
//                    float y2 = currentBeam.position2.y;
                    currentPin.setPosition(correctedPos.x, correctedPos.y);
                    pins.add(currentPin);
                    currentBeam.setEndPin(currentPin);
                    physics.addBeam(currentBeam);
                    currentBeam = new Beam(correctedPos.x, correctedPos.y, correctedPos.x, correctedPos.y);
                    currentBeam.setStartPin(currentPin);
                    currentPin = createPin(correctedPos.x, correctedPos.y);
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
                screenToWorldUnits(x,y, worldPos);
                System.out.println(worldPos);
                if (currentPin != null) // already dragging
                    return false;
                startPos.set(worldPos);
//                float sx = x;
//                float sy = Gdx.graphics.getHeight() - y;
//                float ex = sx;
//                float ey = sy;
                Pin startPin;
                if (overPin != null) {    // if we were over a pin, use that as start
                    startPos.set(overPin.position.x, overPin.position.y);
//                    sx = overPin.position.x;
//                    sy = overPin.position.y;
                    startPin = overPin;
                } else {
                    startPin = createPin(startPos.x, startPos.y);
                    pins.add(startPin);
                }
                currentBeam = new Beam(startPos.x, startPos.y, worldPos.x, worldPos.y);
                currentBeam.setStartPin(startPin);
                beams.add(currentBeam);
                currentPin = createPin(worldPos.x, worldPos.y);

                return false;
            }

            public boolean touchUp(int x, int y, int pointer, int button) {
                if(button == Input.Buttons.RIGHT)
                    return false;

                screenToWorldUnits(x,y, worldPos);
                if(currentBeam.startPin.isOver(worldPos)){   // we didn't move from start position, remove beam
                    beams.removeValue(currentBeam, true);
                } else {
                    // if the dragging ends at an existing pin, snap to that instead of making a new pin
                    if (overPin != null) {
                        worldPos.set(overPin.position.x, overPin.position.y);
                        currentBeam.setEndPosition(worldPos.x, worldPos.y);
                        currentBeam.setEndPin(overPin);
                    } else {
                        pins.add(currentPin);
                        currentBeam.setEndPin(currentPin);
                    }
                    physics.addBeam(currentBeam);
                }
                currentPin = null;
                currentBeam = null;
                return false;
            }
        };
        Gdx.input.setInputProcessor(inputProcessor);

    }


    public void screenToWorldUnits(int x, int y, Vector2 worldPos){
        worldPos.set((x-Gdx.graphics.getWidth()/2)/32f, (Gdx.graphics.getHeight()/2-y)/32f);
    }

    /** check if the mouse is over a pin and if so highlight it and assign it to 'overPin' */
    private void checkOverPin(Vector2 mousePosition) {
        if (overPin != null)
            overPin.sprite.setColor(Color.WHITE);
        // check if the mouse is over a pin and if so highlight it
        overPin = null;
        for (Pin pin : pins) {
            if (pin.isOver(mousePosition)) {
                overPin = pin;
                overPin.sprite.setColor(Color.RED);
                //System.out.println("highlight pin "+overPin.id);
                break;
            }
        }
    }

    private final Array<Beam> beamsToDelete = new Array<>();


    private Pin createPin(float x, float y){
        return createPin(x,y, false);
    }

    private Pin createPin(float x, float y, boolean anchor){
        Pin pin = new Pin(x,y, anchor);
        if(anchor)
            pin.body = physics.addAnchor(pin);
        else
            pin.body = physics.addPin(pin);
        return pin;
    }


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

        if(runPhysics) {
            physics.update(delta);
            physics.updatePinPositions();
        }

        ScreenUtils.clear(Color.TEAL);
        spriteBatch.setProjectionMatrix(camera.combined);

        spriteBatch.begin();

        for(Beam beam : beams){
            beam.sprite.draw(spriteBatch);
        }
        for(Pin pin : pins){
            pin.sprite.draw(spriteBatch);
        }

        spriteBatch.end();

        physics.debugRender(camera);
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        // Resize your screen here. The parameters represent the new window size.
    }

    public void populate(){
        Pin anchor1 = createPin(12, 9, true);
        pins.add(anchor1);

        Pin anchor2 = createPin(20, 12, true);
        pins.add(anchor2);

    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.

    }
}
