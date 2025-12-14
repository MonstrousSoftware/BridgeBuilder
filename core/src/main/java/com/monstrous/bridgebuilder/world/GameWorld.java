package com.monstrous.bridgebuilder.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.monstrous.bridgebuilder.physics.Physics;

import java.io.StringWriter;

public class GameWorld implements Json.Serializable {

    public Array<Pin> pins;
    public Array<Beam> beams;
    public Flag flag;
    public Vehicle vehicle;
    public float zoom;
    public int width;
    public int height;
    public String levelName;
    public int cost;


    public GameWorld() {
        pins = new Array<>();
        beams = new Array<>();
        levelName = "level name";
        zoom = 1.0f;
        cost = 0;
    }

    public void save( String fileName)
    {
        Json json = new Json(JsonWriter.OutputType.json);
        JsonWriter writer = new JsonWriter(new StringWriter());
        json.setWriter(writer);
        json.addClassTag("Pin", Pin.class);
        json.addClassTag("Beam", Beam.class);
        json.addClassTag("Flag", Flag.class);
        json.addClassTag("Vehicle", Vehicle.class);

        FileHandle file = Gdx.files.local(fileName);	// save file
        file.writeString("",  false);	// overwrite

        String s = json.prettyPrint(this);
        file.writeString(s,  true);	// append
    }

    public boolean load( final String fileName, Physics physics )
    {
        Json json = new Json();
        FileHandle file;
        String string;
        System.out.println("loading file: "+fileName);
        file = Gdx.files.internal(fileName);
        try { // save file
            string = file.readString();
        } catch(Exception e) {
            System.out.println("Could not read file: "+fileName);
            return false;
        }
        //System.out.println("loaded: "+string);
        json.addClassTag("Pin", Pin.class);
        json.addClassTag("Beam", Beam.class);
        json.addClassTag("Flag", Flag.class);
        json.addClassTag("Vehicle", Vehicle.class);

        GameWorld loaded = json.fromJson(GameWorld.class, string);
        this.width = loaded.width;
        this.height = loaded.height;
        this.pins = loaded.pins;
        this.beams = loaded.beams;
        this.flag = loaded.flag;
        this.zoom = loaded.zoom;
        this.levelName = loaded.levelName;
        this.cost = loaded.cost;

        for(Pin pin: pins){
            physics.addPin(pin);
        }
        for(Beam beam: beams){
            beam.setStartPin( findPinById(beam.startId) );
            beam.setEndPin( findPinById(beam.endId) );
            beam.updatePosition();
            //System.out.println("beam: "+beam.position1+" to "+beam.position2+" len: "+beam.length+" "+beam.isDeck);
            physics.addBeam(beam);
        }
        physics.addFlag(flag);
        return true;
        //System.out.println("Loaded "+pins.size+" pins and "+beams.size+" beams.");
    }

    private Pin findPinById(int id){
        for(Pin pin: pins){
            if(pin.id == id)
                return pin;
        }
        return null;
    }

    @Override
    public void write(Json json) {
        json.writeValue("width", width);
        json.writeValue("height", height);
        json.writeValue("zoom", zoom);
        json.writeValue("pins", pins);
        json.writeValue("beams", beams);
        json.writeValue("flag", flag);
        json.writeValue("levelName", levelName);
        json.writeValue("cost", cost);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        width = json.readValue("width", Integer.class, 30, jsonData);
        height = json.readValue("height", Integer.class, 20, jsonData);
        pins= json.readValue("pins", Array.class, Integer.class, jsonData);
        beams = json.readValue("beams", Array.class, Beam.class, jsonData);
        flag = json.readValue("flag", Flag.class, jsonData);
        //vehicle = json.readValue("vehicle", Vehicle.class, jsonData);
        zoom = json.readValue("zoom", Float.class, 1.0f, jsonData);
        levelName = json.readValue("levelName", String.class, "level name", jsonData);
        cost = json.readValue("cost", Integer.class, 0, jsonData);
        //System.out.println("read pins: "+pins.size);
    }

}
