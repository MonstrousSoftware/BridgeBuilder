package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

public class Sounds {

    private static Sound jingleSound;
    private static Sound breakSound;
    private static Sound dropSound;
    private static Sound fanfareSound;
    private static Sound fireworksSound;

    public Sounds(AssetManager assets) {
        jingleSound = assets.get("sounds/jingle.ogg");
        breakSound = assets.get("sounds/break.ogg");
        dropSound = assets.get("sounds/groundContact.ogg");
        fanfareSound = assets.get("sounds/fanfare.ogg");
        fireworksSound = assets.get("sounds/fireworks.ogg");
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

    public static void playFireworks(){
        fireworksSound.play();
    }

    public static void playJingle(){
        jingleSound.loop();
    }

    public static void stopJingle(){
        jingleSound.stop();
    }

}
