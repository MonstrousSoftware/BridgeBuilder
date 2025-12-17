package com.monstrous.bridgebuilder;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;


public class ParticleEffects {

    ParticleEffect snowEffect;
    ParticleEffect fireworksEffect;
    ParticleEffectPool snowEffectPool;
    ParticleEffectPool fireworksEffectPool;
    Array<ParticleEffectPool.PooledEffect> effects;
    private int width, height;
    private boolean running = false;

    public ParticleEffects() {
        effects = new Array<>();
        TextureAtlas particleAtlas = new TextureAtlas(Gdx.files.internal("pfx/snow.atlas"));
        snowEffect = new ParticleEffect();
        snowEffect.load(Gdx.files.internal("pfx/snow.p"), particleAtlas);
        snowEffectPool = new ParticleEffectPool(snowEffect, 1, 20);

        fireworksEffect = new ParticleEffect();
        fireworksEffect.load(Gdx.files.internal("pfx/fireworks.p"), particleAtlas);
        fireworksEffectPool = new ParticleEffectPool(fireworksEffect, 1, 20);

        running = false;
    }

    public void resize(int width, int height){
        this.width = width;
        this.height = height;
        if(running) {
            //stop();
            startSnow();    // restart the snow on resize because it depends on window width
        }
    }

    public void startSnow(){
        for(int x = 0; x < width; x+= 200) {
            ParticleEffectPool.PooledEffect effect = snowEffectPool.obtain();
            effect.setPosition(x, height);
            effect.scaleEffect(0.7f, 0.7f * height / (float) width, 0.7f);
            effect.start();
            effects.add(effect);
        }
        running = true;
    }

    public void startFireworks(float x, float y){
        if(effects.size > 40)   // cap number of effects (they may be continuous, i.e. never ending)
            return;

        ParticleEffectPool.PooledEffect effect = fireworksEffectPool.obtain();
        effect.setPosition(x, y); //width/2f, height/2f);
        effect.scaleEffect(0.3f, 0.3f * height / (float) width, 0.3f);
        effect.start();
        effects.add(effect);

        System.out.println("number of particle effects: "+effects.size);
    }

    public void draw(SpriteBatch batch, float deltaTime){
        if(deltaTime > 0.05f)   // cap the delta time, e.g. after browser minimize
            deltaTime = 0.05f;

        for (int i = effects.size - 1; i >= 0; i--) {
            ParticleEffectPool.PooledEffect effect = effects.get(i);
            effect.draw(batch, deltaTime);

            if (effect.isComplete()) {
                effect.free();
                effects.removeIndex(i);
            }
        }
    }

    public void stop(){
        for (int i = effects.size - 1; i >= 0; i--)
            effects.get(i).free(); //free all the effects back to the pool
        effects.clear(); //clear the current effects array
        running = false;
    }


}
