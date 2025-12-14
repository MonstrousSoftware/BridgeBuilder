package com.monstrous.bridgebuilder;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.monstrous.bridgebuilder.physics.Physics;
import com.monstrous.bridgebuilder.world.*;

import java.io.File;


public class GameScreen extends StdScreenAdapter {
    public static int NO_PB = 999999;
    public static int maxLevelNumber = 5;


    public GameWorld world;

    public Physics physics;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private FrameBuffer fbo;
    private GUI gui;
    private SpriteBatch spriteBatch;
    private SpriteBatch pfxSpriteBatch;
    private ImmediateModeRenderer renderer;
    private ShapeRenderer shapeRenderer;
    private Pin currentPin;             // is not in pins, non-null during dragging
    private Beam currentBeam;           // is in beams
    private Pin overPin;                // highlighted pin, or null
    private final Vector2 worldPos = new Vector2();
    private final Vector2 startPos = new Vector2();
    private final Vector2 correctedPos = new Vector2();
    public boolean runPhysics = false;
    public boolean gameOver = false;
    private BuildMaterial buildMaterial = BuildMaterial.DECK;
    public float zoom = 1;
    public boolean showPhysics = false;
    public int levelNumber;
    private Preferences preferences;
    public int personalBest;
    public ParticleEffects particleEffects;
    private PostFilter postFilter;

    @Override
    public void show() {
        gui = new GUI(this);
        spriteBatch = new SpriteBatch();
        pfxSpriteBatch = new SpriteBatch();
        physics = new Physics(this);
        new Sounds();
        preferences = Gdx.app.getPreferences("BridgeBuilder");
        particleEffects = new ParticleEffects();
        shapeRenderer = new ShapeRenderer();
        postFilter = new PostFilter();



        camera = new OrthographicCamera(); //Gdx.graphics.getWidth()/32f, Gdx.graphics.getHeight()/32f);
        viewport = new FitViewport(30, 20, camera);

        renderer = new ImmediateModeRenderer20(false, true, 0);


        world = new GameWorld();
        //populate();
        levelNumber = 1;

        loadLevel(levelNumber);
        gui.showNextLevel(false);   // unlocked on level completion

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
                currentPin.setPosition(worldPos.x, worldPos.y);
                currentBeam.setEndPosition(worldPos.x, worldPos.y);
                // if the beam gets too long, place a pin and create a new beam
                if(currentBeam.length > currentBeam.getMaxLength()){
                    // we might have overshot the max, so truncate the beam length and get adjusted end position
                    currentBeam.truncateLength();
                    correctedPos.set(currentBeam.position2.x, currentBeam.position2.y);
                    currentPin.setPosition(correctedPos.x, correctedPos.y);
                    snapPinToGrid(currentPin);
                    physics.addPin(currentPin);
                    world.pins.add(currentPin);
                    currentBeam.setEndPosition(currentPin.position.x, currentPin.position.y);
                    currentBeam.setEndPin(currentPin);
                    addBeam(currentBeam);

                    currentBeam = new Beam(currentPin.position.x, currentPin.position.y, currentPin.position.x, currentPin.position.y);
                    currentBeam.setMaterial(buildMaterial);
                    currentBeam.setStartPin(currentPin);
                    currentPin = new Pin(currentPin.position.x, currentPin.position.y);
                    world.beams.add(currentBeam);
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
                if (currentPin != null) // already dragging
                    return false;
                screenToWorldUnits(x,y, worldPos);
                System.out.println(worldPos);
                startPos.set(worldPos);
                Pin startPin;
                if (overPin != null) {    // if we were over a pin, use that as start
                    startPos.set(overPin.position.x, overPin.position.y);
                    startPin = overPin;
                } else {
                    startPin = new Pin(startPos.x, startPos.y);
                    snapPinToGrid(startPin);
                    physics.addPin(startPin);
                    world.pins.add(startPin);
                }
                // use snapped position to start beam
                currentBeam = new Beam(startPin.position.x, startPin.position.y, worldPos.x, worldPos.y);
                currentBeam.setMaterial(buildMaterial);
                currentBeam.setStartPin(startPin);
                world.beams.add(currentBeam);
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
                    world.beams.removeValue(currentBeam, true);
                } else {
                    // if the dragging ends at an existing pin, snap to that instead of making a new pin
                    if (overPin != null) {
                        worldPos.set(overPin.position.x, overPin.position.y);
                        currentBeam.setEndPosition(worldPos.x, worldPos.y);
                        currentBeam.setEndPin(overPin);
                    } else {
                        snapPinToGrid(currentPin);
                        physics.addPin(currentPin);
                        world.pins.add(currentPin);
                        currentBeam.setEndPosition(currentPin.position.x, currentPin.position.y);
                        currentBeam.setEndPin(currentPin);
                    }
                    addBeam(currentBeam);
                }
                currentPin = null;
                currentBeam = null;
                return false;
            }

            // don't allow zoom because it screws up the game layout
//            @Override
//            public boolean scrolled(float amountX, float amountY) {
//                zoom += 0.1f*amountY;
//                zoom = MathUtils.clamp(zoom, 0.3f, 5.0f);
//                setCameraView(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//                return true;
//            }
        };
        InputMultiplexer im = new InputMultiplexer();
        im.addProcessor(gui.stage);
        im.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(im);

    }

    private void snapPinToGrid(Pin pin){
        float x = Math.round(pin.position.x);
        float y = Math.round(pin.position.y);
        pin.setPosition(x,y);
    }

    public void screenToWorldUnits(int x, int y, Vector2 worldPos){
        worldPos.set(x,y);
        viewport.unproject(worldPos);
    }

    /** check if the mouse is over a pin and if so highlight it and assign it to 'overPin' */
    private void checkOverPin(Vector2 mousePosition) {
        if (overPin != null)
            overPin.sprite.setColor(Color.WHITE);
        // check if the mouse is over a pin and if so highlight it
        overPin = null;
        for (Pin pin : world.pins) {
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
        for(Beam beam : world.beams){
            if(beam.attachedToPin(pinToDelete)) {
                physics.destroyBeam(beam);
                beamsToDelete.add(beam);
                world.cost -= beam.getCost();
            }
        }
        world.beams.removeAll(beamsToDelete, true);

        world.pins.removeValue(pinToDelete, true);
        physics.destroyPin(pinToDelete);
    }

    private void addBeam(Beam beam){
        physics.addBeam(beam);
        world.cost += beam.getCost();
    }

    private void destroyBeam(Beam beam){
        physics.destroyBeam(beam);
        world.beams.removeValue(beam, true);
    }

    Color stressColor = new Color();

    public void startSimulation(){
        if(runPhysics)
            return;

        gameOver = false;
        // save construction before running physics
        FileHandle fh = Gdx.files.local("attempt"+levelNumber+".json");
        world.save(fh);
        runPhysics = true;
        addVehicle();
        particleEffects.start();
        Sounds.playJingle();
        world.floor.setShatter(false);
        gui.setStatusLabel("Santa is rolling down the hill...");
    }

    public void stopSimulation(){
        gui.clearEndMessage();
        Sounds.stopJingle();
        runPhysics = false;
        particleEffects.stop();
    }

    public void retry(){
        stopSimulation();
        clear();
        FileHandle file = Gdx.files.local("attempt"+levelNumber+".json");
        world.load(file, physics);
    }

    public void nextLevel(){
        if(levelNumber < maxLevelNumber)
            levelNumber++;
        stopSimulation();

        clear();
        physics.clearStaticBodies();
        loadLevel(levelNumber);
        gui.showNextLevel(false);
    }

    public void previousLevel(){
        if(levelNumber > 1)
            levelNumber--;
        stopSimulation();

        clear();
        physics.clearStaticBodies();
        loadLevel(levelNumber);
        gui.showNextLevel(true);
    }

    public void setBuildMaterial( BuildMaterial mat ){
        buildMaterial = mat;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        if(Gdx.input.isKeyJustPressed(Input.Keys.G)){
            gui.setRunMode(true);
            startSimulation();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.H)){   // halt
            runPhysics = !runPhysics;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.R)){
            gui.setRunMode(false);
            retry();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.C)){
            Sounds.stopJingle();
            runPhysics = false;
            reset();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.N)){
            nextLevel();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.P)){
            previousLevel();
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.S)){
            world.save(Gdx.files.local("savefile"+levelNumber+".json"));
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.L)){
            runPhysics = false;
            clear();
            FileHandle fileHandle = Gdx.files.local("savefile"+levelNumber+".json");
            if(!world.load(fileHandle, physics))
                loadLevel(levelNumber);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)){
            setBuildMaterial(BuildMaterial.DECK);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)){
            setBuildMaterial(BuildMaterial.WOOD);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)){
            setBuildMaterial(BuildMaterial.STEEL);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)){
            setBuildMaterial(BuildMaterial.CABLE);
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.D)){
            showPhysics = !showPhysics;
        }

//        CharArray sb = new CharArray();
//        sb.append(world.levelName);
        //gui.setStatusLabel(world.levelName);

        if(runPhysics) {
            physics.update(delta);
            physics.updatePinPositions(world.pins);
            physics.updateBeamPositions(world.beams);
            for(Beam beam : world.beams){
                //beam.updatePosition();
                testBeamStress(beam);
            }
            if(world.vehicle != null) {
                physics.updateVehiclePosition(world.vehicle, !gameOver);
            }
            physics.updateFlagPositions(world.flag);
        }


        viewport.apply();

        if(runPhysics){
            fbo.begin();
            Color lightBlue = Color.BLUE;
            Color darkBlue = Color.NAVY;
            shapeRenderer.setProjectionMatrix(gui.stage.getViewport().getCamera().combined);    // use screenviewport
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), lightBlue, lightBlue, darkBlue, darkBlue);
            shapeRenderer.end();
        } else {
            ScreenUtils.clear(Color.TEAL);
            renderGrid();
        }
        spriteBatch.setProjectionMatrix(camera.combined);

        spriteBatch.begin();


        if(world.vehicle != null)
            world.vehicle.sprite.draw(spriteBatch);

        for(Beam beam : world.beams){
            beam.sprite.draw(spriteBatch);
        }
        for(Pin pin : world.pins){
            pin.draw(spriteBatch);
        }
        world.flag.draw(spriteBatch);
        world.floor.draw(spriteBatch);

        spriteBatch.end();

        pfxSpriteBatch.begin();
        particleEffects.draw(pfxSpriteBatch, delta);
        pfxSpriteBatch.end();


        if(runPhysics){
            fbo.end();
            postFilter.render(fbo);
        }

        if(showPhysics)
            physics.debugRender(camera);

        gui.draw();

    }

    private void testBeamStress(Beam beam){
        float force;

        if(beam.material == BuildMaterial.DECK){
            // take average force of the two revoluteJoints
            force = 0;
            int denom = 0;
            if(beam.joint != null) {
                force += beam.joint.getReactionForce(1f / Physics.TIME_STEP).len();
                denom++;
                if (force  > beam.material.strength) {
                    gui.setStatusLabel("The bridge collapses");
                    System.out.println("break deck joint at "+force);
                    physics.destroyJoint(beam.joint);
                    Sounds.playBreak();
                    //runPhysics = false;
                    beam.joint = null;
                }
            }
            if(beam.joint2 != null) {
                float force2 = beam.joint2.getReactionForce(1f / Physics.TIME_STEP).len();
                force += force2;
                denom++;
                if (force2  > beam.material.strength) {
                    gui.setStatusLabel("The bridge falls apart");
                    System.out.println("break deck joint2 at "+force2);
                    physics.destroyJoint(beam.joint2);
                    Sounds.playBreak();
                    //runPhysics = false;
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
            if (force > beam.material.strength) {
                gui.setStatusLabel("The bridge can't take it");
                System.out.println("break joint at "+force);
                //runPhysics = false;
                destroyBeam(beam);
                Sounds.playBreak();
                return;
            }
        }
        float nForce = force / beam.material.strength;
        if (nForce > 1f)
            nForce = 1f;

        stressColor.set(nForce, 1f-nForce, 0, 1.0f);
        beam.setColor(stressColor);
    }

    @Override
    public void resize(int width, int height) {
        System.out.println("Resize "+width +" x "+height);
        if(width <= 0 || height <= 0) return;
        if(fbo != null)
            fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        postFilter.resize(width, height);

        //float aspectRatio = width / (float)height;
        viewport.setWorldSize(world.width, world.height); /// aspectRatio);
        viewport.update(width, height);
        particleEffects.resize(width, height);
        pfxSpriteBatch.getProjectionMatrix().setToOrtho2D(0,0, width, height);

        gui.resize(width, height);

        Vector2 screenPos = new Vector2(world.floor.position);
        viewport.project(screenPos);
        postFilter.setReflectionY((int)screenPos.y);
    }

    private void setCameraView(int width, int height){
        camera.viewportWidth = zoom * width/32f;
        camera.viewportHeight = zoom * height/32f;
        camera.update();
    }

    public void addVehicle(){
        Pin startAnchor = world.pins.get(0);
        world.vehicle = new Vehicle();
        world.vehicle.setPosition(startAnchor.position.x-3, startAnchor.position.y+world.vehicle.H/2);
        physics.addVehicle(world.vehicle);
    }

    public void destroyVehicle(){
        physics.destroyVehicle(world.vehicle);
        world.vehicle = null;
    }

    public void loadLevel(int levelNumber){
        gui.clearEndMessage();
        Sounds.stopJingle();
        runPhysics = false;
        clear();
        FileHandle file = Gdx.files.internal("level"+levelNumber+".json");
        world.load(file, physics);
        gui.setStatusLabel(world.levelName);

        //float aspectRatio = Gdx.graphics.getWidth() / (float)Gdx.graphics.getHeight();
        viewport.setWorldSize(world.width, world.height); //width/ aspectRatio);
//        viewport.setMinWorldWidth(world.width);
//        viewport.setMinWorldHeight(world.height);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        //zoom = world.zoom;
        //setCameraView(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());


        for(Pin pin : world.pins){
            if(pin.isAnchor){
                physics.addRamp(pin);
            }
        }
        Vector2 screenPos = new Vector2(world.floor.position);
        viewport.project(screenPos);
        postFilter.setReflectionY((int)screenPos.y);

        personalBest = preferences.getInteger("bestScore"+levelNumber, NO_PB);
        setBuildMaterial(BuildMaterial.DECK);
    }

    public void populate(){
        Pin anchor1 = new Pin(-7, 0, true, 1);
        physics.addPin(anchor1);
        world.pins.add(anchor1);
        physics.addRamp(anchor1);

        Pin anchor2 = new Pin(7, 0, true, 2);
        physics.addPin(anchor2);
        world.pins.add(anchor2);
        physics.addRamp(anchor2);

        world.flag = new Flag(10, 0.5f);
        physics.addFlag(world.flag);
    }

    public void clear(){
        gui.clearEndMessage();
        for(Beam beam : world.beams)
            physics.destroyBeam(beam);
        world.beams.clear();

        for(Pin pin : world.pins){
            physics.destroyPin(pin);
        }
        world.pins.clear();
        physics.destroyFlag(world.flag);
        physics.destroyFloor(world.floor);
        if(world.vehicle != null)
            destroyVehicle();
    }

    public void reset(){

        loadLevel(levelNumber);
    }

    public void flagReached(){
        if(gameOver)
            return;
        //System.out.println("Flag reached");
        gameOver = true;
        gui.showWin();
        gui.setStatusLabel("Santa made it safely across.");
        Sounds.playFanfare();
        gui.showNextLevel(true);

        if(world.cost < personalBest) {
            personalBest = world.cost;
            gui.setStatusLabel("A new personal best");
            System.out.println("New personal best! $"+personalBest);
            preferences.putInteger("bestScore" + levelNumber, personalBest);
            preferences.flush();
        }
    }

    public void floorReached(){
        System.out.println("Floor reached");
        gameOver = true;
        gui.showLoss();
        gui.setStatusLabel("Santa breaks the ice like a shattered mirror..");
        world.floor.setShatter(true);
    }

    public void renderGrid() {

        renderer.begin(camera.combined, GL20.GL_LINES);
        Color lineColor = Color.OLIVE;
        float ww = viewport.getWorldWidth();
        float wh = viewport.getWorldHeight();
        //System.out.println("World size "+ww+"x "+wh);
        int hw = 1 + (int)ww/2;
        int hh = 1 + (int)wh/2;
        for (int x = -hw; x <= hw; x++) {
            //Color lineColor = x % 4 == 0 ? Color.GREEN : Color.OLIVE;
            renderer.color(lineColor);
            renderer.vertex(x, -hh, 0);
            renderer.color(lineColor);
            renderer.vertex(x, hh, 0);
        }
        for (int y = -hh; y <= hh; y++) {
            //Color lineColor = y % 4 == 0 ? Color.GREEN : Color.OLIVE;
            renderer.color(lineColor);
            renderer.vertex(-hw, y, 0);
            renderer.color(lineColor);
            renderer.vertex(hw, y, 0);
        }
        renderer.end();
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
        gui.dispose();
    }



}
