package com.monstrous.bridgebuilder;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJoint;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Array;

public class Physics {
    public static final float TIME_STEP = 1/200f;
    public static final float VEHICLE_MASS = 500f;
    public static final float VEHICLE_TORQUE = 1200f;

    // collision bits
    public static final int DECK_FLAG = 1;
    public static final int PIN_FLAG = 2;
    public static final int VEHICLE_FLAG = 4;
    public static final int FLAG_FLAG = 8;

    private final World world;
    public Box2DDebugRenderer debugRenderer;
    private float accumulator = 0;
    Array<Body> tmpBodies = new Array<Body>();

    public Physics(GameScreen screen) {
        Vector2 gravity = new Vector2(0,-10);
        world = new World(gravity, true);
        world.setContactListener(new MyContactListener(screen));
        debugRenderer = new Box2DDebugRenderer();

        //world.setAutoClearForces(true); // useful?


        // define ground


        // Create our body definition
        BodyDef groundBodyDef = new BodyDef();
        // Set its world position
        groundBodyDef.position.set(new Vector2(0, -7));

        // Create a body from the definition and add it to the world
        Body groundBody = world.createBody(groundBodyDef);
        groundBody.setUserData(new Floor());
        PolygonShape groundBox = new PolygonShape();
        groundBox.setAsBox(40, 0.5f);
        groundBody.createFixture(groundBox, 0.0f);
        groundBox.dispose();
    }

    public void addStartRamp(Pin pin){
        BodyDef rampBodyDef = new BodyDef();
        rampBodyDef.position.set(pin.position.x-10, pin.position.y);
        Body rampBody = world.createBody(rampBodyDef);
        rampBody.setUserData(new Ramp());
        PolygonShape rampBox = new PolygonShape();
        rampBox.setAsBox(10, 0.5f);
        rampBody.createFixture(rampBox, 0.0f);
        rampBox.dispose();
    }
    public void addEndRamp(Pin pin){
        BodyDef rampBodyDef = new BodyDef();
        rampBodyDef.position.set(pin.position.x+10, pin.position.y);
        Body rampBody = world.createBody(rampBodyDef);
        rampBody.setUserData(new Ramp());
        PolygonShape rampBox = new PolygonShape();
        rampBox.setAsBox(10, 0.5f);
        rampBody.createFixture(rampBox, 0.0f);
        rampBox.dispose();
    }

    public void addFlag(Flag flag){
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(flag.position.x, flag.position.y+1);
        Body body = world.createBody(bodyDef);
        body.setUserData(flag);
        PolygonShape box = new PolygonShape();
        box.setAsBox(0.5f, 1f);
        Fixture fixture = body.createFixture(box, 0.0f);
        fixture.setSensor(true);

        box.dispose();
    }



    public Body addPin(Pin pin){
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = pin.isAnchor? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(pin.position.x, pin.position.y);


        Body body = world.createBody(bodyDef);
        body.setUserData(pin);

        CircleShape circle = new CircleShape();
        circle.setRadius(pin.W/2f);

        if(pin.isAnchor){
            // Create our fixture and attach it to the body
            body.createFixture(circle, 0f);
        } else {

            // Create a fixture definition to apply our shape to
            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = circle;
            fixtureDef.density = 5.5f;
            fixtureDef.friction = 0.4f;
            fixtureDef.restitution = 0.6f; // Make it bounce a little bit
            fixtureDef.filter.categoryBits = PIN_FLAG;

            // Create our fixture and attach it to the body
            Fixture fixture = body.createFixture(fixtureDef);
        }
        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        circle.dispose();
        pin.body = body;
        return body;
    }

    public void destroyPin(Pin pin){
        world.destroyBody(pin.body);
    }


    public void addVehicle(Vehicle vehicle){
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(vehicle.position.x, vehicle.position.y);

        Body body = world.createBody(bodyDef);
        body.setUserData(vehicle);

        //body.setAngularVelocity(-3);

        CircleShape circle = new CircleShape();
        float radius = vehicle.W/2f;
        circle.setRadius(radius);

        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = VEHICLE_MASS / ((float)Math.PI * radius * radius);
        fixtureDef.friction = 0.4f;
        fixtureDef.restitution = 0.1f; // Make it bounce a little bit
        fixtureDef.filter.categoryBits = VEHICLE_FLAG;
        fixtureDef.filter.maskBits = DECK_FLAG;

        // Create our fixture and attach it to the body
        Fixture fixture = body.createFixture(fixtureDef);
        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        circle.dispose();
        vehicle.body = body;
     }

     public void updateVehiclePosition(Vehicle vehicle, boolean keepMoving){
         Body b = vehicle.body;
         vehicle.setPosition(b.getPosition().x, b.getPosition().y);
         vehicle.setRotation(b.getAngle());

         if(keepMoving) {
             b.applyTorque(-VEHICLE_TORQUE, true);        // force wheel to turn
             b.setAngularDamping(0f);
         }
         else
             b.setAngularDamping(2f);     // stop rolling

     }


    public void destroyVehicle(Vehicle v){
        world.destroyBody(v.body);
    }

    public void addBeam(Beam beam){
        if(beam.isDeck)
            addDeck(beam);
        else
            addSupport(beam);
    }

    public void addSupport(Beam beam){
        Pin a = beam.startPin;
        Pin b = beam.endPin;

        DistanceJointDef defJoint = new DistanceJointDef ();
        defJoint.length = beam.length;
        defJoint.frequencyHz = 30f;
        defJoint.dampingRatio = 1f;

        defJoint.initialize(a.body, b.body, a.position, b.position); // anchor points
        beam.joint = (DistanceJoint) world.createJoint(defJoint); // Returns subclass Joint.
    }

    /** a deck is modeled as a dynamic body with a box shape with revolute joints to both pins */
    public void addDeck(Beam beam){
        Pin a = beam.startPin;
        Pin b = beam.endPin;

        BodyDef deckBodyDef = new BodyDef();
        deckBodyDef.type = BodyDef.BodyType.DynamicBody;
        Vector2 centre = new Vector2();
        centre.set(a.position).add(b.position).scl(0.5f);
        Body deckBody = world.createBody(deckBodyDef);
        deckBody.setUserData(beam);

        PolygonShape deckBox = new PolygonShape();

        // shorten the body a bit (-0.5f) compared to the visual deck so that the body has room to move wrt its neighbours
        // otherwise it will collide and wedge itself
        deckBox.setAsBox(beam.length/2 - 0.4f, beam.H/2f, Vector2.Zero, 0);
        deckBody.setTransform(centre, beam.angle);

        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = deckBox;
        fixtureDef.density = 5.5f;
        fixtureDef.friction = 0.4f;
        fixtureDef.restitution = 0.1f; // Make it bounce a little bit
        fixtureDef.filter.categoryBits = DECK_FLAG;

        // Create our fixture and attach it to the body
        Fixture fixture = deckBody.createFixture(fixtureDef);

        deckBox.dispose();
        beam.body = deckBody;

        RevoluteJointDef defJoint = new RevoluteJointDef();
        defJoint.initialize(a.body, beam.body, a.position);
        defJoint.collideConnected = false;

        beam.joint = world.createJoint(defJoint);
        defJoint.initialize(b.body, beam.body, b.position);
        defJoint.collideConnected = false;
        beam.joint2 = world.createJoint(defJoint);
    }

    public void destroyBeam(Beam beam){
        // destroy joints before destroying attached bodies
        if(beam.joint != null) // structure or deck
            world.destroyJoint(beam.joint);
        if(beam.joint2 != null) // deck
            world.destroyJoint(beam.joint2);
        if(beam.body != null)   // deck
            world.destroyBody(beam.body);   // this will also destroy the joints
        beam.joint = null;
        beam.joint2 = null;
        beam.body = null;
    }

    public void destroyJoint(Joint joint){
        world.destroyJoint(joint);
    }


    public void update(float deltaTime) {
        // fixed time step
        // max frame time to avoid spiral of death (on slow devices)
        float frameTime = Math.min(deltaTime, 0.25f);
        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, 6, 2);
            accumulator -= TIME_STEP;
        }

    }

    public void debugRender(Camera camera){
        debugRenderer.render(world, camera.combined);
    }

    public void updatePinPositions(Array<Pin> pins){
        for(Pin pin : pins ) {
            Body b = pin.body;
            pin.setPosition(b.getPosition().x, b.getPosition().y);
        }
    }

    private final Vector2 p1 = new Vector2();
    private final Vector2 p2 = new Vector2();

    public void updateBeamPositions(Array<Beam> beams){
        for(Beam beam: beams) {
            if(beam.isDeck) {
                if(beam.body != null) {
                    Body b = beam.body;

                    float halfLen = beam.length / 2f;
                    p1.set(-halfLen, 0);
                    p1.set(b.getWorldVector(p1));
                    p2.set(halfLen, 0);
                    p2.set(b.getWorldVector(p2));
                    Vector2 centre = b.getWorldCenter();
                    p1.add(centre);
                    p2.add(centre);
                    beam.setPositions(p1, p2);
                }
            } else if( beam.joint != null ){
                p1.set(beam.joint.getAnchorA());
                p2.set(beam.joint.getAnchorB());
                beam.setPositions(p1, p2);
            }
        }
    }

}
