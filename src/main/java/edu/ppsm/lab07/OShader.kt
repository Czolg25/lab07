package edu.ppsm.lab07

import android.opengl.GLES20

object OShader {
    private const val vertexTexShader = "attribute vec4 a_Position;\n" +
            "attribute vec2 a_Texture;\n" +
            "attribute float a_Texture_id;\n" +
            "varying vec2 v_textureCoordinate;\n" +
            "varying float v_texture_id;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "void main()\n" +
            "{\n" +
            "    v_textureCoordinate = a_Texture;\n" +
            "    v_texture_id = a_Texture_id;\n" +
            "    gl_Position = uMVPMatrix * a_Position;\n" +
            "}\n"
    private const val fragmentTexShader = "precision mediump float;\n" +
            "uniform sampler2D s_texture[4];\n" +
            "varying vec2 v_textureCoordinate;\n" +
            "varying float v_texture_id;\n" +
            "void main(void)\n" +
            "{\n" +
            "    int id = int(v_texture_id);\n" +
            "    gl_FragColor = texture2D(s_texture[id], v_textureCoordinate);\n" +
            "}\n"

    fun loadShader(type: Int,shaderCode:String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader,shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun createShader() :Int{
        val vertShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexTexShader)
        val fragShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentTexShader)
        val renderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(renderProgram,vertShader)
        GLES20.glAttachShader(renderProgram,fragShader)
        GLES20.glLinkProgram(renderProgram)
        return renderProgram
    }
}