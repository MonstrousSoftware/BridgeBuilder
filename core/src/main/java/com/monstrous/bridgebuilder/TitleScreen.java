package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class TitleScreen extends StdScreenAdapter {

    Stage stage;
    Skin skin;
    TextButton startButton;

    Main game;
    Texture texture;
    SpriteBatch batch;

    public TitleScreen(Main main) {
        this.game = main;
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/bridge.json"));
        texture = new Texture("textures/title.png");

        game.assets.load("atlas/bridge.atlas", TextureAtlas.class );
        game.assets.load("textures/spanner.png", Pixmap.class );
        game.assets.load("sounds/break.ogg", Sound.class );
        game.assets.load("sounds/fanfare.ogg", Sound.class );
        game.assets.load("sounds/groundContact.ogg", Sound.class );
        game.assets.load("sounds/jingle.ogg", Sound.class );
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        batch = new SpriteBatch();

        stage.clear();
        Table screenTable = new Table();
        screenTable.setFillParent(true);

        // Material buttons (these should act as radio buttons and highlight the selected one)
        //
        startButton = new TextButton("Start", skin);
        startButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("Starting");
                start();

            }
        });
        startButton.setVisible(false);
        screenTable.add(startButton).bottom().pad(40).width(200).expand();

        stage.addActor(screenTable);
    }

    private void start(){
        System.out.println("Starting");
        game.setScreen(new GameScreen(game));
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        stage.getViewport().update(width, height, true);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        float progress = game.assets.getProgress();

        if(game.assets.update()){
            // finished loading
            startButton.setVisible(true);
        } else {
            System.out.println("Loading "+(int)(progress * 100));
        }


        ScreenUtils.clear(Color.TEAL);

        batch.begin();
        float x = 0.5f*(Gdx.graphics.getWidth() - texture.getWidth());
        float y = 0.5f*(Gdx.graphics.getHeight() - texture.getHeight());
        batch.draw(texture, x, y);
        batch.end();

        stage.act();
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
    }
}
