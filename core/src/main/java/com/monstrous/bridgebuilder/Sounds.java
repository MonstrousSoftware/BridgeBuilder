package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

// to do should use asset manager
public class Sounds implements Disposable {

    private static Sound jingleSound;
    private static Sound breakSound;
    private static Sound dropSound;
    private static Sound fanfareSound;

    public Sounds() {
        jingleSound = Gdx.audio.newSound(Gdx.files.internal("sounds/jingle.ogg"));
        breakSound = Gdx.audio.newSound(Gdx.files.internal("sounds/break.ogg"));
        dropSound = Gdx.audio.newSound(Gdx.files.internal("sounds/groundContact.ogg"));
        fanfareSound = Gdx.audio.newSound(Gdx.files.internal("sounds/fanfare.ogg"));
    }

    public static void playDrop(){
        dropSound.stop();
        dropSound.play();
    }

    public static void playBreak(){
        breakSound.play();
    }

    public static void playFanfare(){
        fanfareSound.play();
    }


    public static void playJingle(){
        jingleSound.loop();
    }

    public static void stopJingle(){
        jingleSound.stop();
    }


    @Override
    public void dispose() {
        jingleSound.dispose();
        breakSound.dispose();
        dropSound.dispose();
        fanfareSound.dispose();
    }
}
