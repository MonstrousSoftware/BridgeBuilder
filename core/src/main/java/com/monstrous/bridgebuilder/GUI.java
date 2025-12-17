package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.monstrous.bridgebuilder.world.BuildMaterial;

import static com.badlogic.gdx.math.Interpolation.bounceOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

public class GUI implements Disposable {

    public final Stage stage;
    private final Skin skin;
    private GameScreen gameScreen;
    private Label statusLabel;
    private Image winImage;
    private Image lossImage;
    TextButton deckButton;
    TextButton steelButton;
    TextButton woodButton;
    TextButton cableButton;
    TextButton modeButton;
    TextButton prevButton;
    TextButton nextButton;
    Label costLabel;
    Label pbLabel;
    private boolean runMode;    // we are either in Edit mode or Run mode

    public GUI(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/bridge.json"));

        statusLabel = new Label("...", skin);
        TextureAtlas atlas = gameScreen.game.assets.get("atlas/bridge.atlas");
        winImage = new Image(atlas.findRegion("hooray3"));
        lossImage = new Image(atlas.findRegion("ohno"));
        costLabel = new Label("0", skin);
        pbLabel = new Label("N/A", skin);
        fillStage();
        runMode = false;
    }

    /** adapt gui to mode */
    public void setRunMode(boolean mode){
        runMode = mode;
        if(runMode) {
            modeButton.setText("Retry");
            //modeButton.setChecked(false);
            deckButton.setVisible(false);
            steelButton.setVisible(false);
            woodButton.setVisible(false);
            cableButton.setVisible(false);
            //statusLabel.setVisible(false);
        } else {
            modeButton.setText("Go!");
            //modeButton.setChecked(false);
            deckButton.setVisible(true);
            steelButton.setVisible(true);
            woodButton.setVisible(true);
            cableButton.setVisible(true);
            //statusLabel.setVisible(true);
        }
    }

    public void setBuildMaterial(BuildMaterial buildMaterial){
        deckButton.setChecked(buildMaterial == BuildMaterial.DECK);
        woodButton.setChecked(buildMaterial == BuildMaterial.WOOD);
        steelButton.setChecked(buildMaterial == BuildMaterial.STEEL);
        cableButton.setChecked(buildMaterial == BuildMaterial.CABLE);
    }

    private void fillStage(){
        stage.clear();
        //stage.setDebugAll(true);
        Table screenTable = new Table();
        screenTable.setFillParent(true);


        // Material buttons (these should act as radio buttons and highlight the selected one)
        //
        deckButton = new TextButton("Deck", skin);
        deckButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                gameScreen.setBuildMaterial(BuildMaterial.DECK);
            }
        });

        woodButton = new TextButton("Wood", skin);
        woodButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                gameScreen.setBuildMaterial(BuildMaterial.WOOD);
            }
        });

        steelButton = new TextButton("Steel", skin);
        steelButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                gameScreen.setBuildMaterial(BuildMaterial.STEEL);
            }
        });

        cableButton = new TextButton("Cable", skin);
        cableButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                gameScreen.setBuildMaterial(BuildMaterial.CABLE);
            }
        });


        setBuildMaterial(gameScreen.buildMaterial);

        ButtonGroup<TextButton> buttonGroup = new ButtonGroup(deckButton, woodButton, steelButton, cableButton);
        buttonGroup.setMinCheckCount(1);
        buttonGroup.setMaxCheckCount(1);
        buttonGroup.setUncheckLast(true);

        // button toggles between Edit mode and Run mode
        // todo should not appear in Down position during run mode
        modeButton = new TextButton("Go!", skin);
        modeButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                if(runMode) {
                    gameScreen.retry();
                } else {
                    gameScreen.startSimulation();
                }
                runMode = !runMode;
                setRunMode(runMode);
            }
        });

        nextButton = new TextButton("Next", skin);
        nextButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                setRunMode(false);
                gameScreen.nextLevel();
            }
        });

        prevButton = new TextButton("Previous", skin);
        prevButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                setRunMode(false);
                gameScreen.previousLevel();
            }
        });




        Table materials = new Table();
        materials.add(deckButton).width(100).height(60);
        materials.add(woodButton).width(100).height(60);
        materials.add(steelButton).width(100).height(60);
        materials.add(cableButton).width(100).height(60);

        Table buttonLine = new Table();
        buttonLine.add(materials);
        buttonLine.add().expandX();
        buttonLine.add(prevButton).width(100).height(60);
        buttonLine.add(nextButton).width(100).height(60);
        buttonLine.add(modeButton).width(100).height(60);

        // $ 800                          Best: $ 150
        Table topLine = new Table();
        topLine.add(new Label("$", skin)).pad(5);
        topLine.add(costLabel).pad(5).width(100);
        topLine.add().expandX();
        topLine.add(new Label("Best: $", skin)).pad(5);
        topLine.add(pbLabel).pad(5).width(100);

        screenTable.add(topLine).fillX().top().row();

        screenTable.add(statusLabel).pad(10).left().expandX();
        screenTable.row();
        screenTable.add(buttonLine).fillX().pad(10).bottom().expandY();

        stage.addActor(screenTable);
    }


    public void draw(){
        costLabel.setText(gameScreen.world.cost);
        if(gameScreen.personalBest == GameScreen.NO_PB)
            pbLabel.setText("----");
        else
            pbLabel.setText(gameScreen.personalBest);
        prevButton.setVisible(gameScreen.levelNumber > 1);

        stage.act(Gdx.graphics.getDeltaTime());
        stage.getViewport().apply();
        stage.draw();
    }

    public void resize(int width, int height){
        stage.getViewport().update(width, height, true);
    }

    public void setStatusLabel(String stat){
        statusLabel.setText(stat);
    }

    public void showWin(){
        showEndMessage(winImage);
    }

    public void showLoss(){
        showEndMessage(lossImage);
    }

    private void showEndMessage(Image image){
        int x = (int)(0.4f*(stage.getWidth()-image.getWidth()));
        int y = (int)(0.55*stage.getHeight());

        stage.addActor(image);
        image.addAction(
            parallel(
                sequence(moveTo(x, 0, 0), moveTo(x, y, 2, bounceOut)),
                sequence(scaleTo(.2f, .2f), scaleTo(1.2f, 1.2f, 2, bounceOut))));
    }

    public void clearEndMessage(){
        winImage.remove();
        lossImage.remove();
    }

    public void showNextLevel(boolean show){
        nextButton.setVisible(show);
    }

    @Override
    public void dispose() {
        skin.dispose();
        stage.dispose();
    }
}
