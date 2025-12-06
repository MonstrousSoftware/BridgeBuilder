package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;

public class GUI implements Disposable {
    public final Stage stage;
    private final Skin skin;
    private Label status;

    public GUI() {
        stage = new Stage();
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));


        status = new Label("...", skin);
        fillStage();
    }

    private void fillStage(){
        stage.clear();
        Table screen = new Table();
        screen.setFillParent(true);

        screen.add(status).pad(10).bottom().left().expand();


        stage.addActor(screen);
    }

    public void draw(){
        stage.draw();
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
