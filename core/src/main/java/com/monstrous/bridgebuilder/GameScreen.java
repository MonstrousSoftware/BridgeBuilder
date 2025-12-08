package com.monstrous.bridgebuilder;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;

import static java.lang.Float.isNaN;


public class GameScreen extends ScreenAdapter {
    //public static float COLOR_SCALE = 1000;
    public static float BREAK_FORCE = 15000f;

    public Array<Pin> pins;
    public Array<Beam> beams;
    public Vehicle vehicle;
    public GameWorld world;

    public Physics physics;
    private OrthographicCamera camera;
    private GUI gui;


    private SpriteBatch spriteBatch;
    private ImmediateModeRenderer renderer;
    private Pin currentPin;             // is not in pins, non-null during dragging
    private Beam currentBeam;           // is in beams
    private Pin overPin;                // highlighted pin, or null
    private Vector2 worldPos = new Vector2();
    private Vector2 startPos = new Vector2();
    private Vector2 correctedPos = new Vector2();
    public boolean runPhysics = false;
    public boolean deckMode = true; // are beams decks? otherwise they are supports


    @Override
    public void show() {
        gui = new GUI(this);
        spriteBatch = new SpriteBatch();
        physics = new Physics();

        // for debug renderer
        camera = new OrthographicCamera(Gdx.graphics.getWidth()/32f, Gdx.graphics.getHeight()/32f);

        renderer = new ImmediateModeRenderer20(false, true, 0);

        pins = new Array<>();
        beams = new Array<>();
        populate();
        world = new GameWorld();
        world.set(pins, beams);
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
                    currentPin.setPosition(correctedPos.x, correctedPos.y);
                    physics.addPin(currentPin);
                    pins.add(currentPin);
                    currentBeam.setEndPin(currentPin);
                    physics.addBeam(currentBeam);
                    currentBeam = new Beam(correctedPos.x, correctedPos.y, correctedPos.x, correctedPos.y);
                    currentBeam.setDeck(deckMode);
                    currentBeam.setStartPin(currentPin);
                    currentPin = new Pin(correctedPos.x, correctedPos.y);
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
                Pin startPin;
                if (overPin != null) {    // if we were over a pin, use that as start
                    startPos.set(overPin.position.x, overPin.position.y);
                    startPin = overPin;
                } else {
                    startPin = new Pin(startPos.x, startPos.y);
                    physics.addPin(startPin);
                    pins.add(startPin);
                }
                currentBeam = new Beam(startPos.x, startPos.y, worldPos.x, worldPos.y);
                currentBeam.setDeck(deckMode);
                currentBeam.setStartPin(startPin);
                beams.add(currentBeam);
                currentPin = new Pin(worldPos.x, worldPos.y);

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
                        physics.addPin(currentPin);
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
        InputMultiplexer im = new InputMultiplexer();
        im.addProcessor(gui.stage);
        im.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(im);

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


    private void deletePin( Pin pinToDelete ){
        if(pinToDelete.isAnchor)    // cannot delete anchors
            return;
        pins.removeValue(pinToDelete, true);
        physics.destroyPin(pinToDelete);

        // remove all attached beams
        beamsToDelete.clear();
        for(Beam beam : beams){
            if(beam.attachedToPin(pinToDelete))
                beamsToDelete.add(beam);
        }
        beams.removeAll(beamsToDelete, true);
    }

    private void deleteBeam(Beam beam){
        physics.destroyBeam(beam);
        beams.removeValue(beam, true);
    }

    Color stressColor = new Color();

    public void startSimulation(){
        world.save("attempt.json");
        runPhysics = true;
        addVehicle();
    }

    public void retry(){
        runPhysics = false;
        clear();
        world.load("attempt.json", physics);
        pins = world.pins;
        beams = world.beams;
    }

    @Override
    public void render(float delta) {
        if(Gdx.input.isKeyJustPressed(Input.Keys.G)){
            runPhysics = !runPhysics;
            if(runPhysics)
                addVehicle();
            else
                destroyVehicle();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.R)){
            runPhysics = false;
            reset();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.S)){
            world.save("savefile.json");
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.L)){
            runPhysics = false;
            clear();
            world.load("savefile.json", physics);
            pins = world.pins;
            beams = world.beams;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)){
            deckMode = true;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)){
            deckMode = false;
        }

        if(runPhysics) {
            physics.update(delta);
            physics.updatePinPositions(pins);
            physics.updateBeamPositions(beams);
            for(Beam beam : beams){
                //beam.updatePosition();
                testBeamStress(beam);
            }
            if(vehicle != null)
                physics.updateVehiclePosition(vehicle);
        }

        ScreenUtils.clear(Color.TEAL);
        //renderGrid();
        spriteBatch.setProjectionMatrix(camera.combined);

        spriteBatch.begin();

        for(Beam beam : beams){
            beam.sprite.draw(spriteBatch);
        }
        for(Pin pin : pins){
            pin.sprite.draw(spriteBatch);
        }
        if(vehicle != null)
            vehicle.sprite.draw(spriteBatch);

        spriteBatch.end();

        physics.debugRender(camera);

        StringBuilder sb = new StringBuilder();
//        for(Beam beam : beams) {
//            if(beam.joint == null)
//                continue;
//            Vector2 forceVec = beam.joint.getReactionForce(1f/Physics.TIME_STEP);
//            float force = forceVec.len();
//            sb.append("[");
//            sb.append(force);
//            sb.append("]");
//        }


        gui.setStatus(sb.toString());
        gui.draw();
    }

    private void testBeamStress(Beam beam){

        float force;


        if(beam.isDeck){
            force = 0;
            int denom = 0;
            if(beam.joint != null) {
                Vector2 forceVec = beam.joint.getReactionForce(1f / Physics.TIME_STEP);
                force += forceVec.len();
                denom++;
                if (forceVec.len()  > BREAK_FORCE) {
                    physics.destroyJoint(beam.joint);
                    beam.joint = null;
                }
            }
            if(beam.joint2 != null) {
                Vector2 forceVec2 = beam.joint2.getReactionForce(1f / Physics.TIME_STEP);
                force += forceVec2.len();
                denom++;
                if (forceVec2.len()  > BREAK_FORCE) {
                    physics.destroyJoint(beam.joint2);
                    beam.joint2 = null;
                }
            }
            if(denom > 0)
                force /= (float)denom;  // use average force on joints for colouring
        } else {
            if(beam.joint == null)
                return;
            Vector2 forceVec = beam.joint.getReactionForce(1f / Physics.TIME_STEP);
            force = forceVec.len();
            if (force > BREAK_FORCE) {
                deleteBeam(beam);
                return;
            }
        }
        float nForce = force / BREAK_FORCE;
        if (nForce > 1f)
            nForce = 1f;

        stressColor.set(nForce, 1f-nForce, 0, 1.0f);
        beam.setColor(stressColor);
    }

    @Override
    public void resize(int width, int height) {
        if(width <= 0 || height <= 0) return;

        // todo think about viewport behaviour
        camera.viewportWidth = width/32f;
        camera.viewportHeight = height/32f;
        camera.update();

        gui.resize(width, height);
    }

    public void addVehicle(){
        Pin startAnchor = pins.get(0);
        vehicle = new Vehicle(startAnchor.position.x-3, startAnchor.position.y+2);
        physics.addVehicle(vehicle);
    }

    public void destroyVehicle(){
        physics.destroyVehicle(vehicle);
        vehicle = null;
    }


    public void populate(){
        Pin anchor1 = new Pin(-8, 0, true);
        physics.addPin(anchor1);
        pins.add(anchor1);
        physics.addStartRamp(anchor1);

        Pin anchor2 = new Pin(9, 3, true);
        physics.addPin(anchor2);
        pins.add(anchor2);
        physics.addEndRamp(anchor2);

        //addVehicle();
    }

    public void clear(){
        for(Pin pin : pins){
            physics.destroyPin(pin);
        }
        pins.clear();
        for(Beam beam : beams)
            physics.destroyBeam(beam);
        beams.clear();
        if(vehicle != null)
            destroyVehicle();
    }

    public void reset(){
        clear();
        populate();
    }

    public void renderGrid() {

        renderer.begin(camera.combined, GL20.GL_LINES);

        for (int x = -20; x <= 20; x++) {
            renderer.color(Color.GREEN);
            renderer.vertex(x, -10, 0);
            renderer.color(Color.LIGHT_GRAY);
            renderer.vertex(x, 10, 0);
        }
        for (int y = -10; y <= 10; y++) {
            renderer.color(Color.GREEN);
            renderer.vertex(-20, y, 0);
            renderer.color(Color.GREEN);
            renderer.vertex(20, y, 0);
        }
        renderer.end();
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
        gui.dispose();

    }



}
