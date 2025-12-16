package com.monstrous.bridgebuilder;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.monstrous.bridgebuilder.physics.Physics;
import com.monstrous.bridgebuilder.world.*;


public class GameScreen extends StdScreenAdapter {
    public static int NO_PB = 999999;           // no personal best so far ("infinite" cost)
    public static int maxLevelNumber = 5;
    public static Color HIGHLIGHT_COLOR = Color.GREEN;
    public static float Y_OFF = -10f;    // offset from drag position


    public Main game;
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
    private Beam overBeam;
    private final Vector2 worldPos = new Vector2();
    private final Vector2 startPos = new Vector2();
    private final Vector2 correctedPos = new Vector2();
    public boolean runPhysics = false;
    public boolean gameOver = false;
    public BuildMaterial buildMaterial = BuildMaterial.DECK;
    public float zoom = 1;
    public boolean showPhysics = false;
    public int levelNumber;
    private Preferences preferences;
    public int personalBest;
    public ParticleEffects particleEffects;
    private PostFilter postFilter;
    public Images images;


    public GameScreen(Main game) {
        this.game = game;

        TextureAtlas atlas = game.assets.get("atlas/bridge.atlas");
        images = new Images(atlas);

    }

    @Override
    public void show() {
        gui = new GUI(this);
        spriteBatch = new SpriteBatch();
        pfxSpriteBatch = new SpriteBatch();
        physics = new Physics(this);
        new Sounds(game.assets);
        preferences = Gdx.app.getPreferences("BridgeBuilder");
        particleEffects = new ParticleEffects();
        shapeRenderer = new ShapeRenderer();
        postFilter = new PostFilter();


        Pixmap pixmap = game.assets.get("textures/spanner.png");
        // Set hotspot to the middle of it (0,0 would be the top-left corner)
        int xHotspot = 15, yHotspot = 15;
        Cursor cursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot);
        pixmap.dispose(); // We don't need the pixmap anymore
        Gdx.graphics.setCursor(cursor);

        camera = new OrthographicCamera(); //Gdx.graphics.getWidth()/32f, Gdx.graphics.getHeight()/32f);
        viewport = new FitViewport(30, 20, camera);

        renderer = new ImmediateModeRenderer20(false, true, 0);


        world = new GameWorld();
        //populate();
        levelNumber = 1;

        loadLevel(levelNumber);
        gui.showNextLevel(false);   // unlocked on level completion

        GestureDetector.GestureListener gestureListener = new GestureDetector.GestureAdapter() {


            @Override
            public boolean touchDown(float x, float y, int pointer, int button) {
                //System.out.println("touch down "+x +" x "+y);
                if(runPhysics)
                    return false;
                followMouse(x, y);  // on a touch screen we don't receive mouseMoved()
                screenToWorldUnits(x, y, startPos);  // keep track of drag start
                return false;
            }

            @Override
            public boolean tap(float x, float y, int count, int button) {
                System.out.println("tap "+x +" x "+y+" count:"+count);
                if(count == 1 && button == Input.Buttons.LEFT){ // place a pin
                    followMouse(x, y);  // update overPin
                    if (overPin == null) {    // don't place on top of another pin
                        screenToWorldUnits(x, y, startPos);
                        Pin pin = new Pin(startPos.x, startPos.y);
                        snapPinToGrid(pin);
                        addPin(pin);
//                        physics.addPin(pin);
//                        world.pins.add(pin);
                    }
                    return true;
                }
                if(count == 2 || button == Input.Buttons.RIGHT){ // double tap to take out pin or beam (or RMB tap)
                    if(overBeam != null){
                        deleteBeam(overBeam);
                        overBeam = null;
                    }
                    if(overPin != null) {
                        deletePin(overPin);
                        overPin = null;
                    }
                }
                return super.tap(x, y, count, button);
            }

            @Override
            public boolean pan(float x, float y, float deltaX, float deltaY) {
                //System.out.println("pan "+x +" x "+y);
                if(runPhysics)
                    return false;

                if (currentPin == null) { // start dragging
                    // use startPos and overPin from touchDown() as x - deltaX is not reliable
                    Pin startPin;
                    if (overPin != null) {    // if we were over a pin, use that as start
                        startPos.set(overPin.position.x, overPin.position.y);
                        startPin = overPin;
                    } else {
                        startPin = new Pin(startPos.x, startPos.y);
                        snapPinToGrid(startPin);
                        addPin(startPin);
                    }

                    screenToWorldUnits(x,y, worldPos);  // get current drag position which may differ from startPos
                    // use snapped position to start beam (instead of startPos)
                    currentBeam = new Beam(startPin.position.x, startPin.position.y, worldPos.x, worldPos.y);
                    currentBeam.setMaterial(buildMaterial);
                    currentBeam.setStartPin(startPin);
                    world.beams.add(currentBeam);
                    currentPin = new Pin(worldPos.x, worldPos.y);   // this pin will follow the mouse
                    return true;
                }

                // continue dragging
                followMouse(x, y);  // on a touch screen we don't receive mouseMoved()
                // show the dragged end above the touch location so that the user's finger doesn't obscure the view.
                screenToWorldUnits(x,y+Y_OFF, worldPos);

                currentPin.setPosition(worldPos.x, worldPos.y);
                currentBeam.setEndPosition(worldPos.x, worldPos.y);
                // if the beam gets too long, place a pin and create a new beam
                if(currentBeam.length > currentBeam.getMaxLength()){
                    // we might have overshot the max, so truncate the beam length and get adjusted end position
                    currentBeam.truncateLength();
                    correctedPos.set(currentBeam.position2.x, currentBeam.position2.y);
                    currentPin.setPosition(correctedPos.x, correctedPos.y);
                    snapPinToGrid(currentPin);
                    addPin(currentPin);
                    currentBeam.setEndPosition(currentPin.position.x, currentPin.position.y);
                    currentBeam.setEndPin(currentPin);
                    addBeam(currentBeam);

                    currentBeam = new Beam(currentPin.position.x, currentPin.position.y, currentPin.position.x, currentPin.position.y);
                    currentBeam.setMaterial(buildMaterial);
                    currentBeam.setStartPin(currentPin);
                    currentPin = new Pin(currentPin.position.x, currentPin.position.y);
                    world.beams.add(currentBeam);
                }
                return true;
            }

            @Override
            public boolean panStop(float x, float y, int pointer, int button) {
                System.out.println("panStop "+x +" x "+y);
                if(runPhysics)
                    return false;

                screenToWorldUnits(x,y+Y_OFF, worldPos);
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
                        addPin(currentPin);
                        currentBeam.setEndPosition(currentPin.position.x, currentPin.position.y);
                        currentBeam.setEndPin(currentPin);
                    }
                    addBeam(currentBeam);
                }
                currentPin = null;
                currentBeam = null;
                return true;
            }

            @Override
            public boolean zoom(float initialDistance, float distance) {
                if(runPhysics)
                    return false;
                zoom *= 0.2f * distance/initialDistance;
                zoom = MathUtils.clamp(zoom, 0.5f, 2.0f);
                viewport.setWorldSize(zoom*world.width, zoom*world.height);
                System.out.println("zoom "+zoom);
                return true;
            }
        };

        InputAdapter inputProcessor = new InputAdapter() {

            @Override
            // on a touchscreen this will not get called, so call followMouse() again in the pan and touchDown events
            public boolean mouseMoved(int x, int y) {
                followMouse(x, y);
                return true;
            }


            @Override
            public boolean scrolled(float amountX, float amountY) {
                if(runPhysics)
                    return false;
                zoom += 0.1f*amountY;
                zoom = MathUtils.clamp(zoom, 0.5f, 2.0f);
                viewport.setWorldSize(zoom*world.width, zoom*world.height);
                return true;
            }
        };
        InputMultiplexer im = new InputMultiplexer();
        im.addProcessor(gui.stage);
        im.addProcessor(new GestureDetector(gestureListener));
        im.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(im);

    }

    /** highlights pin or beam that the mouse is over, sets variables overPin and overBeam */
    private void followMouse(float screenX, float screenY){
        screenToWorldUnits(screenX, screenY, worldPos);
        checkOverPin(worldPos);
        checkOverBeam(worldPos);
    }

    private void snapPinToGrid(Pin pin){
        float x = Math.round(pin.position.x);
        float y = Math.round(pin.position.y);
        pin.setPosition(x,y);
    }

    public void screenToWorldUnits(float x, float y, Vector2 worldPos){
        worldPos.set(x,y);
        viewport.unproject(worldPos);
    }

    /** check if the mouse is over a pin and if so highlight it and assign it to 'overPin' */
    private void checkOverPin(Vector2 mousePosition) {
        if (overPin != null)    // unhighlight the "over" pin before testing again
            overPin.sprite.setColor(Color.WHITE);
        // check if the mouse is over a pin and if so highlight it
        overPin = null;
        for (Pin pin : world.pins) {
            if (pin.isOver(mousePosition)) {
                overPin = pin;
                overPin.sprite.setColor(HIGHLIGHT_COLOR);
                //System.out.println("highlight pin "+overPin.id);
                break;
            }
        }
    }

    private void checkOverBeam(Vector2 mousePosition) {
        if (overBeam != null)    // unhighlight the "over" pin before testing again
            overBeam.sprite.setColor(Color.WHITE);
        // check if the mouse is over a pin and if so highlight it
        overBeam = null;
        for (Beam beam : world.beams) {
            if (beam.isOver(mousePosition)) {
                overBeam = beam;
                overBeam.sprite.setColor(HIGHLIGHT_COLOR);
                //System.out.println("highlight pin "+overPin.id);
                break;
            }
        }
    }

    private final Array<Beam> beamsToDelete = new Array<>();


    private void addPin(Pin pin){
        physics.addPin(pin);
        world.pins.add(pin);
        world.cost += pin.getCost();
    }

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
        world.cost -= pinToDelete.getCost();
    }

    private void addBeam(Beam beam){
        physics.addBeam(beam);
        world.cost += beam.getCost();
    }

    private void deleteBeam(Beam beam){
        world.cost -= beam.getCost();
        physics.destroyBeam(beam);
        world.beams.removeValue(beam, true);
    }

    private void destroyBeam(Beam beam){
        physics.destroyBeam(beam);
        world.beams.removeValue(beam, true);
    }

    Color stressColor = new Color();

    public void startSimulation(){
        if(runPhysics)
            return;
        // reset zoom to 1.0
        zoom = 1.0f;
        viewport.setWorldSize(world.width, world.height);

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
        gui.setStatusLabel("");
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


        if(runPhysics) {
            physics.update(delta);
            physics.updatePinPositions(world.pins);
            physics.updateBeamPositions(world.beams);
            for(Beam beam : world.beams){
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
            postFilter.setReflectionY((world.height/2f + world.floor.position.y)/world.height);
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

        viewport.setWorldSize(world.width, world.height);
        viewport.update(width, height);

        if(fbo != null)
            fbo.dispose();
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        postFilter.resize(width, height);


        particleEffects.resize(width, height);
        pfxSpriteBatch.getProjectionMatrix().setToOrtho2D(0,0, width, height);

        gui.resize(width, height);


        postFilter.setReflectionY((world.height/2f + world.floor.position.y)/world.height);
    }

//    private void setCameraView(int width, int height){
//        camera.viewportWidth = zoom * width/32f;
//        camera.viewportHeight = zoom * height/32f;
//        camera.update();
//    }

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
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        postFilter.setReflectionY((world.height/2f + world.floor.position.y)/world.height);

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
        addPin(anchor1);
        physics.addRamp(anchor1);

        Pin anchor2 = new Pin(7, 0, true, 2);
        addPin(anchor2);
        physics.addRamp(anchor2);

        world.flag = new Tree(10, 0.5f);
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
        if(levelNumber == maxLevelNumber)
            gui.setStatusLabel("You have completed all levels.");
        else
            gui.setStatusLabel("Santa made it safely across.");
        Sounds.playFanfare();
        gui.showNextLevel(levelNumber < maxLevelNumber);

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
