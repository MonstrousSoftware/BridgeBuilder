package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.badlogic.gdx.math.Interpolation.bounceOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

public class GUI implements Disposable {

    public final Stage stage;
    private final Skin skin;
    private GameScreen gameScreen;
    private Label status;
    private Image winImage;
    private Image lossImage;
    TextButton deckButton;
    TextButton structureButton;
    TextButton cableButton;
    TextButton modeButton;
    TextButton nextButton;
    Label costLabel;
    Label pbLabel;
    private boolean runMode;    // we are either in Edit mode or Run mode

    public GUI(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        status = new Label("...", skin);
        winImage = new Image(new Texture(Gdx.files.internal("textures/hooray.png")));
        lossImage = new Image(new Texture(Gdx.files.internal("textures/ohno.png")));
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
            deckButton.setVisible(false);
            structureButton.setVisible(false);
            cableButton.setVisible(false);
        } else {
            modeButton.setText("Go!");
            deckButton.setVisible(true);
            structureButton.setVisible(true);
            cableButton.setVisible(true);
        }
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

        structureButton = new TextButton("Structure", skin);
        structureButton.addListener(new ChangeListener() {
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


        // button toggles between Edit mode and Run mode
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

        nextButton = new TextButton("Next Level", skin);
        nextButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                setRunMode(false);
                gameScreen.nextLevel();
            }
        });




        Table materials = new Table();
        materials.add(deckButton).width(100).pad(10);
        materials.add(structureButton).width(100).pad(10);
        materials.add(cableButton).width(100).pad(10);

        Table buttonLine = new Table();
        buttonLine.add(materials);
        buttonLine.add().expandX();
        buttonLine.add(nextButton).width(100).pad(10);
        buttonLine.add(modeButton).width(100).pad(10);

        Table costTable = new Table();
        costTable.add(new Label("$", skin)).pad(5);
        costTable.add(costLabel).pad(5).width(100);

        Table pbTable = new Table();
        pbTable.add(new Label("Personal Best: $", skin)).pad(5);
        pbTable.add(pbLabel).pad(5).width(100);

        Table topLine = new Table();
        topLine.add(costTable).left().expandX();
        topLine.add(pbTable).right();

        screenTable.add(topLine).top().row();


        screenTable.add(buttonLine).fillX().pad(10).bottom().expandY();
        screenTable.row();
//
        screenTable.add(status).pad(10).left().expandX();

        stage.addActor(screenTable);

    }


    public void draw(){
        costLabel.setText(gameScreen.world.cost);
        if(gameScreen.personalBest == GameScreen.NO_PB)
            pbLabel.setText("----");
        else
            pbLabel.setText(gameScreen.personalBest);

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int width, int height){
        stage.getViewport().update(width, height, true);
    }

    public void setStatus(String stat){
        status.setText(stat);
    }

    public void showWin(){
        showEndMessage(winImage);
    }

    public void showLoss(){
        showEndMessage(lossImage);
    }

    private void showEndMessage(Image image){
        int x = (int)(0.5f*(stage.getWidth()-image.getWidth()));
        int y = (int)(0.6*stage.getHeight());

        stage.addActor(image);
        image.addAction(
            parallel(
                sequence(moveTo(x, 0, 0), moveTo(x, y, 2, bounceOut)),
                sequence(scaleTo(.2f, .2f), scaleTo(1.5f, 1.5f, 2, bounceOut))));
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
