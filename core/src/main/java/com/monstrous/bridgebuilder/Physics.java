package com.monstrous.bridgebuilder;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJoint;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.badlogic.gdx.utils.Array;

public class Physics {
    public static final float TIME_STEP = 1/200f;

    private final World world;
    public Box2DDebugRenderer debugRenderer;
    private float accumulator = 0;
    Array<Body> tmpBodies = new Array<Body>();

    public Physics() {
        Vector2 gravity = new Vector2(0,-10);
        world = new World(gravity, true);
        debugRenderer = new Box2DDebugRenderer();

        // define ground


        // Create our body definition
        BodyDef groundBodyDef = new BodyDef();
        // Set its world position
        groundBodyDef.position.set(new Vector2(0, -7));

        // Create a body from the definition and add it to the world
        Body groundBody = world.createBody(groundBodyDef);

        // Create a polygon shape
        PolygonShape groundBox = new PolygonShape();
        // Set the polygon shape as a box which is twice the size of our view port and 20 high
        // (setAsBox takes half-width and half-height as arguments)
        groundBox.setAsBox(20, 0.5f);
        // Create a fixture from our polygon shape and add it to our ground body
        groundBody.createFixture(groundBox, 0.0f);
        // Clean up after ourselves
        groundBox.dispose();
    }

    public void addStartRamp(Pin pin){
        BodyDef rampBodyDef = new BodyDef();
        rampBodyDef.position.set(pin.position.x-10, pin.position.y);
        Body rampBody = world.createBody(rampBodyDef);
        PolygonShape rampBox = new PolygonShape();
        rampBox.setAsBox(10, 0.5f);
        rampBody.createFixture(rampBox, 0.0f);
        rampBox.dispose();
    }
    public void addEndRamp(Pin pin){
        BodyDef rampBodyDef = new BodyDef();
        rampBodyDef.position.set(pin.position.x+10, pin.position.y);
        Body rampBody = world.createBody(rampBodyDef);
        PolygonShape rampBox = new PolygonShape();
        rampBox.setAsBox(10, 0.5f);
        rampBody.createFixture(rampBox, 0.0f);
        rampBox.dispose();
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

        body.setAngularVelocity(-1);

        CircleShape circle = new CircleShape();
        circle.setRadius(vehicle.W/2f);

        // Create a fixture definition to apply our shape to
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 5.5f;
        fixtureDef.friction = 0.4f;
        fixtureDef.restitution = 0.1f; // Make it bounce a little bit

        // Create our fixture and attach it to the body
        Fixture fixture = body.createFixture(fixtureDef);
        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        circle.dispose();
        vehicle.body = body;
     }

     public void updateVehiclePosition(Vehicle vehicle){
         Body b = vehicle.body;
         vehicle.setPosition(b.getPosition().x, b.getPosition().y);
     }


    public void destroyVehicle(Vehicle v){
        world.destroyBody(v.body);
    }

    public void addBeam(Beam beam){
        Pin a = beam.startPin;
        Pin b = beam.endPin;

        DistanceJointDef defJoint = new DistanceJointDef ();
        defJoint.length = beam.length;
        defJoint.frequencyHz = 10f;
        defJoint.dampingRatio = 1f;

        defJoint.initialize(a.body, b.body, a.position, b.position); // anchor points
        beam.joint = (DistanceJoint) world.createJoint(defJoint); // Returns subclass Joint.
    }

    public void destroyBeam(Beam beam){
        world.destroyJoint(beam.joint);
        beam.joint = null;
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

}
