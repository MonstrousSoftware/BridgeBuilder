package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class GUI implements Disposable {

    public final Stage stage;
    private final Skin skin;
    private GameScreen gameScreen;
    private Label status;

    public GUI(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        stage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        status = new Label("...", skin);
        fillStage();
    }

    private void fillStage(){
        stage.clear();
        Table screenTable = new Table();
        screenTable.setFillParent(true);

        screenTable.add(status).pad(10).bottom().left().expand();

        TextButton startButton = new TextButton("Start", skin);
        startButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                gameScreen.startSimulation();
            }
        });
        TextButton retryButton = new TextButton("Retry", skin);
        retryButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                gameScreen.retry();
            }
        });
        screenTable.add(startButton).pad(10).bottom();
        screenTable.add(retryButton).pad(10).bottom();


        stage.addActor(screenTable);
    }

    public void draw(){
        stage.draw();
    }

    public void resize(int width, int height){
        stage.getViewport().update(width, height, true);
    }

    public void setStatus(String stat){
        status.setText(stat);
    }

    @Override
    public void dispose() {
        skin.dispose();
        stage.dispose();
    }
}
