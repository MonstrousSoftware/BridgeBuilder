package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {

    public AssetManager assets;

    @Override
    public void create() {
        assets = new AssetManager();

        setScreen(new TitleScreen(this));
    }
}
