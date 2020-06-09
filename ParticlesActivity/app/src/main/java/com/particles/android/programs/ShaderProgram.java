package com.particles.android.programs;

import android.content.Context;

import com.particles.android.util.ShaderHelper;
import com.particles.android.util.TextResourcesReader;

import static android.opengl.GLES20.glUseProgram;

public class ShaderProgram {

    //Uniform constants
    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";

    //Attribute constants
    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";

    protected static final String U_COLOR = "u_Color";

    protected static final String U_TIME = "u_Time";

    protected static final String A_DIRECTION_VECTOR = "a_DirectionVector";
    protected static final String A_PARTICLE_START_TIME = "a_ParticleStartTime";

    //Shader program
    protected final int program;

    protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
        program = ShaderHelper.buildProgram(TextResourcesReader.readTextFileFromResource(context, vertexShaderResourceId)
                , TextResourcesReader.readTextFileFromResource(context, fragmentShaderResourceId));
    }

    public void useProgram() {
        glUseProgram(program);
    }


}
