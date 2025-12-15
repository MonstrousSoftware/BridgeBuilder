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

    private static final float BAR_WIDTH = 300;
    private static final float BAR_HEIGHT = 20f;

    Stage stage;
    Skin skin;
    TextButton startButton;

    Main game;
    Texture texture;
    Texture whitePixel;
    SpriteBatch batch;

    public TitleScreen(Main main) {
        this.game = main;
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/bridge.json"));
        texture = new Texture("textures/title.png");
        // Create loading segment part, use Pixmap to generate the texture
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        whitePixel = new Texture(pm);
        pm.dispose();

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

        if(game.assets.update(10)){
            // finished loading
            startButton.setVisible(true);
        }


        ScreenUtils.clear(Color.TEAL);

        batch.begin();
        float x = 0.5f*(Gdx.graphics.getWidth() - texture.getWidth());
        float y = 0.5f*(Gdx.graphics.getHeight() - texture.getHeight());
        batch.draw(texture, x, y);

        // Draw the loading bar
        float barX = 0.5f*(Gdx.graphics.getWidth() - BAR_WIDTH);
        float barY = BAR_HEIGHT;
        batch.setColor(Color.DARK_GRAY);
        batch.draw(whitePixel, barX, barY, BAR_WIDTH, BAR_HEIGHT);
        batch.setColor(Color.GREEN);
        batch.draw(whitePixel, barX, barY, progress * BAR_WIDTH, BAR_HEIGHT);
        batch.setColor(Color.WHITE);
        batch.end();

        stage.act();
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
        whitePixel.dispose();
    }
}
