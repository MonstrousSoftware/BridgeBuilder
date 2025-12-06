package com.monstrous.bridgebuilder;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.DistanceJoint;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.badlogic.gdx.utils.Array;

public class Physics {
    public static final float TIME_STEP = 1/60f;

    private final World world;
    public Box2DDebugRenderer debugRenderer;
    private float accumulator = 0;
    Array<Body> tmpBodies = new Array<Body>();

    public Physics() {
        world = new World(new Vector2(0, -10), true);
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
            fixtureDef.density = 0.5f;
            fixtureDef.friction = 0.4f;
            fixtureDef.restitution = 0.6f; // Make it bounce a little bit

            // Create our fixture and attach it to the body
            Fixture fixture = body.createFixture(fixtureDef);
        }
        // Remember to dispose of any shapes after you're done with them!
        // BodyDef and FixtureDef don't need disposing, but shapes do.
        circle.dispose();
        return body;
    }

    public void destroyPin(Pin pin){
        world.destroyBody(pin.body);
    }

    public void addBeam(Beam beam){
        Pin a = beam.startPin;
        Pin b = beam.endPin;

        DistanceJointDef defJoint = new DistanceJointDef ();
        defJoint.length = beam.length;
        defJoint.frequencyHz = 0.8f;
        defJoint.dampingRatio = 1.0f;
        defJoint.initialize(a.body, b.body, new Vector2(0,0), new Vector2(0, 0)); // anchor points??
        DistanceJoint joint = (DistanceJoint) world.createJoint(defJoint); // Returns subclass Joint.
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

    public void updatePinPositions(){

        // Now fill the array with all bodies
        world.getBodies(tmpBodies);

        for (Body b : tmpBodies) {
            // Get the body's user data - in this example, our user
            // data is an instance of the Entity class
            Pin e = (Pin) b.getUserData();

            if (e != null) {
                // Update the entities/sprites position and angle
                e.setPosition(b.getPosition().x, b.getPosition().y);
                // We need to convert our angle from radians to degrees
                //e.setRotation(MathUtils.radiansToDegrees * b.getAngle());
            }
        }
    }

}
