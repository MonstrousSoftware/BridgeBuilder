package com.monstrous.bridgebuilder.physics;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import com.badlogic.gdx.utils.Array;
import com.monstrous.bridgebuilder.GameScreen;
import com.monstrous.bridgebuilder.world.*;

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
    private float time;
    Array<Body> staticBodies = new Array<Body>();
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
        groundBodyDef.position.set(new Vector2(0, -12));

        // Create a body from the definition and add it to the world
        Body groundBody = world.createBody(groundBodyDef);
        //staticBodies.add(groundBody);
        groundBody.setUserData(new Floor());
        PolygonShape groundBox = new PolygonShape();
        groundBox.setAsBox(40, 1.0f);
        groundBody.createFixture(groundBox, 0.0f);
        groundBox.dispose();
    }

    public void addRamp(Pin pin){
        if(pin.anchorDirection == 0)
            return;
        BodyDef rampBodyDef = new BodyDef();
        if(pin.anchorDirection == 1)
            rampBodyDef.position.set(pin.position.x-10, pin.position.y);
        else if(pin.anchorDirection == 2)
            rampBodyDef.position.set(pin.position.x+10, pin.position.y);
        else if(pin.anchorDirection == 3)
            rampBodyDef.position.set(pin.position.x, pin.position.y-5);
        Body rampBody = world.createBody(rampBodyDef);
        staticBodies.add(rampBody);
        rampBody.setUserData(new Ramp());
        PolygonShape rampBox = new PolygonShape();
        if(pin.anchorDirection == 3)
            rampBox.setAsBox(0.5f, 5f);
        else
            rampBox.setAsBox(10, 0.5f);

        rampBody.createFixture(rampBox, 0.0f);
        rampBox.dispose();
    }

    public void clearStaticBodies(){
        for(Body body : staticBodies)
            world.destroyBody(body);
        staticBodies.clear();
    }


    public void addFlag(Flag flag){
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(flag.position.x, flag.position.y+0.5f);

        Body body = world.createBody(bodyDef);
        //staticBodies.add(body);
        body.setUserData(flag);
        flag.body = body;

        PolygonShape box = new PolygonShape();
        box.setAsBox(0.5f, 0.5f);


        CircleShape circle = new CircleShape();
        circle.setRadius(0.5f);

        Fixture fixture = flag.body.createFixture(box, 10.5f);
        //fixture.setSensor(true);

        bodyDef.position.set(flag.position.x, flag.position.y+1.5f);
        flag.body2 = world.createBody(bodyDef);
        flag.body2.setUserData(flag);
        flag.body2.createFixture(circle, 0.5f);

        bodyDef.position.set(flag.position.x, flag.position.y+3.0f);
        flag.body3 = world.createBody(bodyDef);
        flag.body3.setUserData(flag);
        flag.body3.createFixture(circle, 0.5f);


        RevoluteJointDef defJoint = new RevoluteJointDef();
        defJoint.initialize(flag.body, flag.body2, flag.body.getWorldCenter());
        defJoint.collideConnected = false;
        defJoint.enableMotor = true;
        defJoint.lowerAngle =   -0.05f * (float)Math.PI;
        defJoint.upperAngle = 0.05f * (float)Math.PI;
        defJoint.motorSpeed = 0.0f;
        defJoint.maxMotorTorque = 10.1f;
        defJoint.enableLimit = true;
        flag.joint1 = (RevoluteJoint) world.createJoint(defJoint);

        defJoint.initialize(flag.body2, flag.body3, flag.body2.getWorldCenter());
        defJoint.lowerAngle *= 2f;
        defJoint.upperAngle *= 2f;
        flag.joint2 = (RevoluteJoint) world.createJoint(defJoint);

        circle.dispose();
        box.dispose();
    }

    /** update articulated tree segments */
    public void updateFlagPositions(Flag flag){

        //System.out.println(time+"cos:"+Math.cos(time));
        flag.joint1.setMotorSpeed(0.1f*(float) Math.cos(time));
        flag.joint2.setMotorSpeed(0.1f*(float) Math.cos(time));

        flag.sprites.get(0).setOriginBasedPosition(flag.body.getPosition().x, flag.body.getPosition().y);
        flag.sprites.get(1).setOriginBasedPosition(flag.body2.getPosition().x, flag.body2.getPosition().y);
        flag.sprites.get(2).setOriginBasedPosition(flag.body3.getPosition().x, flag.body3.getPosition().y);
        flag.sprites.get(1).setRotation(flag.body2.getAngle() * 180f/(float)Math.PI);
        flag.sprites.get(2).setRotation(flag.body3.getAngle() * 180f/(float)Math.PI);

    }

    public void destroyFlag(Flag flag){
        if(flag == null || flag.body == null)
            return;
        world.destroyBody(flag.body);
        world.destroyBody(flag.body2);
        world.destroyBody(flag.body3);
        flag.body = null;
        flag.body2 = null;
        flag.body3 = null;
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
            fixtureDef.density = 15.5f;
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
        switch(beam.material) {
            case DECK:
                addDeck(beam);
                break;
            case STEEL:
            case WOOD:
                addSupport(beam);
                break;
            case CABLE:
                addRope(beam);
                break;
        }
    }

    public void addRope(Beam beam){
        Pin a = beam.startPin;
        Pin b = beam.endPin;

        RopeJointDef defJoint = new RopeJointDef ();
        defJoint.bodyA = a.body;
        defJoint.localAnchorA.set(a.body.getLocalPoint(a.position));
        defJoint.bodyB = b.body;
        defJoint.localAnchorB.set(b.body.getLocalPoint(b.position));
        defJoint.maxLength = beam.length;

        beam.joint = world.createJoint(defJoint); // Returns subclass Joint.
    }

    public void addSupport(Beam beam){
        Pin a = beam.startPin;
        Pin b = beam.endPin;

        DistanceJointDef defJoint = new DistanceJointDef ();
        defJoint.length = beam.length;
        defJoint.frequencyHz = 30f;
        defJoint.dampingRatio = 1f;

        defJoint.initialize(a.body, b.body, a.position, b.position); // anchor points
        beam.joint = world.createJoint(defJoint); // Returns subclass Joint.
    }

    /** a deck is modeled as a dynamic body with a box shape with revolute joints to both pins */
    public void addDeck(Beam beam){
        Pin a = beam.startPin;
        Pin b = beam.endPin;

        if(beam.length/2 <= 0.5f)
            return;

        BodyDef deckBodyDef = new BodyDef();
        deckBodyDef.type = BodyDef.BodyType.DynamicBody;
        Vector2 centre = new Vector2();
        centre.set(a.position).add(b.position).scl(0.5f);
        Body deckBody = world.createBody(deckBodyDef);
        deckBody.setUserData(beam);

        PolygonShape deckBox = new PolygonShape();

        // shorten the body a bit (-0.5f) compared to the visual deck so that the body has room to move wrt its neighbours
        // otherwise it will collide and wedge itself

        deckBox.setAsBox(beam.length/2 - 0.5f, beam.H/2f, Vector2.Zero, 0);
        deckBody.setTransform(centre, beam.angle);

        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = deckBox;
        fixtureDef.density = 0.5f;  // use a low density so the mass is comparable to the pin's mass (joint bodies should have similar mass)
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
            world.destroyBody(beam.body);
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
            time += TIME_STEP;
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
            if(beam.material == BuildMaterial.DECK) {
                // a deck has a body and may collide
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
                // a structure or cable is just a joint
                p1.set(beam.joint.getAnchorA());
                p2.set(beam.joint.getAnchorB());
                beam.setPositions(p1, p2);
            }
        }
    }

}
