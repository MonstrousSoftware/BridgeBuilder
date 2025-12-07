package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.StringWriter;

public class GameWorld implements Json.Serializable {

    public Array<Pin> pins;
    public Array<Beam> beams;

    public GameWorld() {
    }

    public void set(Array<Pin> pins, Array<Beam> beams){
        this.pins = pins;
        this.beams = beams;
    }

    public void save( String fileName)
    {
        Json json = new Json(JsonWriter.OutputType.json);
        JsonWriter writer = new JsonWriter(new StringWriter());
        json.setWriter(writer);
        json.addClassTag("Pin", Pin.class);
        json.addClassTag("Beam", Beam.class);

        FileHandle file = Gdx.files.local(fileName);	// save file
        file.writeString("",  false);	// overwrite

        // save array
        String s = json.prettyPrint(this);
        file.writeString(s,  true);	// append
        //String s2 = json.prettyPrint(beams);
        //file.writeString(s2,  true);
    }

    public void load( final String fileName, Physics physics )
    {
        //clear();

        Json json = new Json();
        FileHandle file;
        String string;

        file = Gdx.files.local(fileName);	// save file
        string = file.readString();
        json.addClassTag("Pin", Pin.class);
        json.addClassTag("Beam", Beam.class);

        GameWorld loaded = json.fromJson(GameWorld.class, file);
        this.pins = loaded.pins;
        this.beams = loaded.beams;

        for(Pin pin: pins){
            physics.addPin(pin);
        }
    }

    @Override
    public void write(Json json) {
        json.writeValue("pins", pins);
        json.writeValue("beams", beams);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        pins= json.readValue("pins", Array.class, Integer.class, jsonData);
        beams = json.readValue("beams", Array.class, Beam.class, jsonData);
        System.out.println("read pins: "+pins.size);
    }

}
