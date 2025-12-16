package com.monstrous.bridgebuilder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;


/** Post-processing effect to render an FBO to screen applying shader effects.
* Shows a vignette.
* Reflects top part of the screen at the bottom of the screen, the reflection is blended.
*/
public class PostFilter implements Disposable {

    public SpriteBatch batch;
    private ShaderProgram program;
    private float[] resolution = { 640, 480 };
    private int reflectionY = 80;

    public PostFilter() {
        // full screen post processing shader
        program = new ShaderProgram(
            Gdx.files.internal("shaders\\vignette.vertex.glsl"),
            Gdx.files.internal("shaders\\vignette.fragment.glsl"));
        if (!program.isCompiled())
            throw new GdxRuntimeException(program.getLog());
        ShaderProgram.pedantic = false;

        batch = new SpriteBatch();
    }

    public void resize ( int width, int height) {
        resolution[0] = width;
        resolution[1] = height;
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);  // to ensure the fbo is rendered to the full window after a resize
    }

    public void setReflectionY(float yfraction){
        reflectionY = (int)(yfraction * Gdx.graphics.getHeight());
    }

    public void render( FrameBuffer fbo ) {
        Sprite s = new Sprite(fbo.getColorBufferTexture());
        s.flip(false,  true); // coordinate system in buffer differs from screen

        Sprite sMirror = new Sprite(fbo.getColorBufferTexture(), 0, reflectionY, (int)s.getWidth(), (int)(s.getHeight()-reflectionY));
        // keep upside down

        batch.begin();
        batch.disableBlending();
        batch.setShader(program);                        // post-processing shader
        batch.draw(s, 0, 0, resolution[0], resolution[1]);    // draw frame buffer as screen filling texture


        batch.setColor(1,1,1,0.25f);
        batch.enableBlending();

        // draw inverse fbo as mirror effect
        batch.draw(sMirror, 0, 0, resolution[0], reflectionY);
//        batch.disableBlending();
        batch.end();

    }


    @Override
    public void dispose() {
        batch.dispose();
        program.dispose();
    }
}
