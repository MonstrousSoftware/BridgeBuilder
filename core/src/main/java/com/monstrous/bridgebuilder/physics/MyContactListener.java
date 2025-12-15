package com.monstrous.bridgebuilder.physics;

import com.badlogic.gdx.physics.box2d.*;
import com.monstrous.bridgebuilder.GameScreen;
import com.monstrous.bridgebuilder.Sounds;
import com.monstrous.bridgebuilder.world.*;

public class MyContactListener implements ContactListener {
    private final GameScreen gameScreen;

    public MyContactListener(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();
        Body ba = fa.getBody();
        Body bb = fb.getBody();
        Object a = ba.getUserData();
        Object b = bb.getUserData();
        if(isFlagReached(a,b)){
            gameScreen.flagReached();
            return;
        }
        if(isFloorReached(a,b)){
            Sounds.playDrop();
            gameScreen.floorReached();
            return;
        }
        if(a.getClass() == Floor.class || b.getClass() == Floor.class)
            Sounds.playDrop();

        //System.out.println("Begin contact "+describe(a)+" vs "+describe(b));

    }

    private boolean isFlagReached(Object a, Object b){
        if(a == null || b == null)
            return false;
        if(a.getClass() == Vehicle.class && b.getClass() == Tree.class)
            return true;
        if(b.getClass() == Vehicle.class && a.getClass() == Tree.class)
            return true;
        return false;
    }

    private boolean isFloorReached(Object a, Object b){
        if(a == null || b == null)
            return false;
        if(a.getClass() == Vehicle.class && b.getClass() == Floor.class)
            return true;
        if(b.getClass() == Vehicle.class && a.getClass() == Floor.class)
            return true;
        return false;
    }

    private String describe(Object o){
        if(o == null)
            return "null";
        if(o.getClass() == Beam.class){
            return "Beam";
        }
        if(o.getClass() == Pin.class){
            return "Pin";
        }
        if(o.getClass() == Tree.class){
            return "Flag";
        }
        if(o.getClass() == Vehicle.class){
            return "Vehicle";
        }
        return o.getClass().getSimpleName();
    }

    @Override
    public void endContact(Contact contact) {
        //System.out.println("End contact");
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
}
