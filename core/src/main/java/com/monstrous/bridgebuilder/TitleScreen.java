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

/** Present the title screen, load the assets and force user interaction before we start playing sound.
    */
public class TitleScreen extends StdScreenAdapter {

    private static final float BAR_WIDTH = 300;        // loading progress bar
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

        startButton = new TextButton("Start", skin);
        startButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("Starting");
                start();

            }
        });
        startButton.setVisible(false);    // hide until assets loaded
        screenTable.add(startButton).bottom().pad(40).width(200).expand();

        stage.addActor(screenTable);
    }

    private void start(){
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

        if(game.assets.update(10)){    // 10ms per call
            // finished loading
            // show button to proceeed
            // (for web it is important the user interacts before we can play any sound)
            startButton.setVisible(true);
        }


        ScreenUtils.clear(Color.TEAL);

        batch.begin();
        // ensure texture will fit on screen
        int texWidth = Math.min(Gdx.graphics.getWidth(), texture.getWidth());
        int texHeight = Math.min(Gdx.graphics.getHeight(), texture.getHeight());
        float x = 0.5f*(Gdx.graphics.getWidth() - texWidth);
        float y = 0.5f*(Gdx.graphics.getHeight() - texHeight);
        batch.draw(texture, x, y, texWidth, texHeight);

        // Draw the loading bar
        float barX = 0.5f*(Gdx.graphics.getWidth() - BAR_WIDTH);    // centred
        float barY = BAR_HEIGHT;        // close to the bottom
        // draw a grey rectangle
        batch.setColor(Color.DARK_GRAY);
        batch.draw(whitePixel, barX, barY, BAR_WIDTH, BAR_HEIGHT);
        // overlay a green rectangle that grouws in size 
        batch.setColor(Color.GREEN);
        batch.draw(whitePixel, barX, barY, progress * BAR_WIDTH, BAR_HEIGHT);
        // restore default colour
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
