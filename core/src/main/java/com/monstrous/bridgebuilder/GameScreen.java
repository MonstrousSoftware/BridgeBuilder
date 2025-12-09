package com.monstrous.bridgebuilder;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.*;



public class GameScreen extends ScreenAdapter {
    public static float COLOR_SCALE = 15000f;
    public static float BREAK_FORCE = 15000f;


    public enum BuildMaterial { DECK, STRUCTURE };

    public Array<Pin> pins;
    public Array<Beam> beams;
    public Vehicle vehicle;
    public Flag flag;
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
    private BuildMaterial buildMaterial = BuildMaterial.DECK;
    public float zoom = 1;


    @Override
    public void show() {
        gui = new GUI(this);
        spriteBatch = new SpriteBatch();
        physics = new Physics(this);

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
                if(runPhysics)
                    return false;
                screenToWorldUnits(x,y, worldPos);
                checkOverPin(worldPos);
                if(currentPin == null)
                    return false;
                //float sy = Gdx.graphics.getHeight() - y;
                currentPin.setPosition(worldPos.x, worldPos.y);
                currentBeam.setEndPosition(worldPos.x, worldPos.y);
                // if the beam gets too long, place a pin and create a new beam
                if(currentBeam.length > currentBeam.getMaxLength()){
                    // we might have overshot the max, so truncate the beam length and get adjusted end position
                    currentBeam.truncateLength();
                    correctedPos.set(currentBeam.position2.x, currentBeam.position2.y);
                    currentPin.setPosition(correctedPos.x, correctedPos.y);
                    physics.addPin(currentPin);
                    pins.add(currentPin);
                    currentBeam.setEndPin(currentPin);
                    physics.addBeam(currentBeam);
                    currentBeam = new Beam(correctedPos.x, correctedPos.y, correctedPos.x, correctedPos.y);
                    currentBeam.setDeck(buildMaterial == BuildMaterial.DECK);
                    currentBeam.setStartPin(currentPin);
                    currentPin = new Pin(correctedPos.x, correctedPos.y);
                    beams.add(currentBeam);
                }
                return false;
            }

            public boolean touchDown(int x, int y, int pointer, int button) {
                if(runPhysics)
                    return false;
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
                currentBeam.setDeck(buildMaterial == BuildMaterial.DECK);
                currentBeam.setStartPin(startPin);
                beams.add(currentBeam);
                currentPin = new Pin(worldPos.x, worldPos.y);

                return false;
            }

            public boolean touchUp(int x, int y, int pointer, int button) {
                if(runPhysics)
                    return false;
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

            @Override
            public boolean scrolled(float amountX, float amountY) {
                zoom += amountY;
                zoom = MathUtils.clamp(zoom, 1.0f, 3.0f);
                setCameraView(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                return true;
            }
        };
        InputMultiplexer im = new InputMultiplexer();
        im.addProcessor(gui.stage);
        im.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(im);

    }


    private final Vector3 tmpVec3 = new Vector3();

    public void screenToWorldUnits(int x, int y, Vector2 worldPos){
        tmpVec3.set(x,y,0);
        camera.unproject(tmpVec3);
        worldPos.set(tmpVec3.x, tmpVec3.y);
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

        // remove all attached beams
        // since beams have joints they need to be destroyed before the body they are attached to.
        beamsToDelete.clear();
        for(Beam beam : beams){
            if(beam.attachedToPin(pinToDelete)) {
                physics.destroyBeam(beam);
                beamsToDelete.add(beam);
            }
        }
        beams.removeAll(beamsToDelete, true);

        pins.removeValue(pinToDelete, true);
        physics.destroyPin(pinToDelete);
    }

    private void deleteBeam(Beam beam){
        physics.destroyBeam(beam);
        beams.removeValue(beam, true);
    }

    Color stressColor = new Color();

    public void startSimulation(){
        if(runPhysics)
            return;
        world.save("attempt.json");
        runPhysics = true;
        addVehicle();
    }

    public void retry(){
        gui.clearEndMessage();
        runPhysics = false;
        clear();
        world.load("attempt.json", physics);
        pins = world.pins;
        beams = world.beams;
    }

    public void setBuildMaterial( BuildMaterial mat ){
        buildMaterial = mat;
    }

    @Override
    public void render(float delta) {
        if(Gdx.input.isKeyJustPressed(Input.Keys.G)){
            startSimulation();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.R)){
            retry();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.C)){
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
            setBuildMaterial(BuildMaterial.DECK);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)){
            setBuildMaterial(BuildMaterial.STRUCTURE);
        }

        if(runPhysics) {
            physics.update(delta);
            physics.updatePinPositions(pins);
            physics.updateBeamPositions(beams);
            for(Beam beam : beams){
                //beam.updatePosition();
                testBeamStress(beam);
            }
            if(vehicle != null) {
                physics.updateVehiclePosition(vehicle);
            }
        }

        ScreenUtils.clear(Color.TEAL);
        //renderGrid();
        spriteBatch.setProjectionMatrix(camera.combined);

        spriteBatch.begin();

        if(vehicle != null)
            vehicle.sprite.draw(spriteBatch);

        for(Beam beam : beams){
            beam.sprite.draw(spriteBatch);
        }
        for(Pin pin : pins){
            pin.sprite.draw(spriteBatch);
        }
        flag.sprite.draw(spriteBatch);


        spriteBatch.end();

        physics.debugRender(camera);

        StringBuilder sb = new StringBuilder();
        sb.append("Zoom: ");
        sb.append(zoom);
        sb.append(" Beam forces:");
        for(Beam beam : beams){
            if(!beam.isDeck)
                continue;
            if(beam.joint != null) {
                Vector2 forceVec = beam.joint.getReactionForce(1f / Physics.TIME_STEP);
                float force = forceVec.len();
                sb.append((int) force);
                sb.append("+");
            } else
                sb.append("- +");
            if(beam.joint2 != null) {
                Vector2 forceVec = beam.joint2.getReactionForce(1f / Physics.TIME_STEP);
                float force = forceVec.len();
                sb.append((int) force);
                sb.append("   ");
            } else
                sb.append("-   ");
        }

        gui.setStatus(sb.toString());
        gui.draw();
    }

    private void testBeamStress(Beam beam){
        float force;

        if(beam.isDeck){
            // take average force of the two revoluteJoints
            force = 0;
            int denom = 0;
            if(beam.joint != null) {
                force += beam.joint.getReactionForce(1f / Physics.TIME_STEP).len();
                denom++;
                if (force  > BREAK_FORCE) {
                    System.out.println("break joint at "+force);
                    physics.destroyJoint(beam.joint);
                    beam.joint = null;
                }
            }
            if(beam.joint2 != null) {
                float force2 = beam.joint2.getReactionForce(1f / Physics.TIME_STEP).len();
                force += force2;
                denom++;
                if (force2  > BREAK_FORCE) {
                    System.out.println("break joint2 at "+force2);
                    physics.destroyJoint(beam.joint2);
                    beam.joint2 = null;
                }
                force += force2;
            }
            if(denom > 0)
                force /= (float)denom;  // use average force on joints for colouring
        } else {
            if(beam.joint == null)
                return;
            force = beam.joint.getReactionForce(1f / Physics.TIME_STEP).len();
            if (force > BREAK_FORCE) {
                deleteBeam(beam);
                return;
            }
        }
        float nForce = force / COLOR_SCALE;
        if (nForce > 1f)
            nForce = 1f;

        stressColor.set(nForce, 1f-nForce, 0, 1.0f);
        beam.setColor(stressColor);
    }

    @Override
    public void resize(int width, int height) {
        if(width <= 0 || height <= 0) return;

        // todo think about viewport behaviour
        setCameraView(width, height);

        gui.resize(width, height);
        //gui.showLoss();
    }

    private void setCameraView(int width, int height){
        camera.viewportWidth = zoom * width/32f;
        camera.viewportHeight = zoom * height/32f;
        camera.update();
    }

    public void addVehicle(){
        Pin startAnchor = pins.get(0);
        vehicle = new Vehicle();
        vehicle.setPosition(startAnchor.position.x-3, startAnchor.position.y+vehicle.H/2);
        physics.addVehicle(vehicle);
    }

    public void destroyVehicle(){
        physics.destroyVehicle(vehicle);
        vehicle = null;
    }


    public void populate(){
        Pin anchor1 = new Pin(-7, 0, true);
        physics.addPin(anchor1);
        pins.add(anchor1);
        physics.addStartRamp(anchor1);

        Pin anchor2 = new Pin(7, 0, true);
        physics.addPin(anchor2);
        pins.add(anchor2);
        physics.addEndRamp(anchor2);

        flag = new Flag(10, 0.5f);
        physics.addFlag(flag);


        //addVehicle();
    }

    public void clear(){
        for(Beam beam : beams)
            physics.destroyBeam(beam);
        beams.clear();

        for(Pin pin : pins){
            physics.destroyPin(pin);
        }
        pins.clear();

        if(vehicle != null)
            destroyVehicle();
    }

    public void reset(){
        clear();
        populate();
    }

    public void flagReached(){
        System.out.println("Flag reached");
        gui.showWin();
    }

    public void floorReached(){
        System.out.println("Floor reached");
        gui.showLoss();
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
