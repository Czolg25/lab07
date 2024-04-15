package edu.ppsm.lab07

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

// Pamięć jednego wierzchołka: 3geom+2wsp_tex+1id_tex * 4 = 24 bajty
const val NODE_SIZE = (3 + 2 + 1) * 4

class CGLScene(ctx: Context) {
    // Maksymalna liczba trójkątów tworzących scenę
    private val maxTriangles = 1_024

    // Bufor na dane
    private val buffer: FloatBuffer

    // Współrzędne punktów i tekstur sześcianu
    private val cubeCoords = floatArrayOf( // ściana 1 (X Y -1 S T  Tid)
        -1f, -1f, -1f, 0f, 0f, 0.1f,
        -1f, 1f, -1f, 0f, 0.5f, 0.1f,
        1f, -1f, -1f, 0.5f, 0f, 0.1f,
        -1f, 1f, -1f, 0f, 0.5f, 0.1f,
        1f, 1f, -1f, 0.5f, 0.5f, 0.1f,
        1f, -1f, -1f, 0.5f, 0f, 0.1f,  // ściana 4 (X Y +1 S T  Tid)
        -1f, -1f, 1f, 0f, 0f, 0.1f,
        -1f, 1f, 1f, 0f, 0.5f, 0.1f,
        1f, -1f, 1f, 0.5f, 0f, 0.1f,
        -1f, 1f, 1f, 0f, 0.5f, 0.1f,
        1f, 1f, 1f, 0.5f, 0.5f, 0.1f,
        1f, -1f, 1f, 0.5f, 0f, 0.1f,  //ściana 2 (-1 Y Z S T  Tid)
        -1f, -1f, -1f, 0.5f, 0f, 0.1f,
        -1f, -1f, 1f, 0.5f, 0.5f, 0.1f,
        -1f, 1f, -1f, 1f, 0f, 0.1f,
        -1f, 1f, -1f, 1f, 0f, 0.1f,
        -1f, -1f, 1f, 0.5f, 0.5f, 0.1f,
        -1f, 1f, 1f, 1f, 0.5f, 0.1f,  //ściana 5 (+1 Y Z S T  Tid)
        1f, -1f, -1f, 0.5f, 0f, 0.1f,
        1f, 1f, -1f, 1f, 0f, 0.1f,
        1f, -1f, 1f, 0.5f, 0.5f, 0.1f,
        1f, 1f, -1f, 1f, 0f, 0.1f,
        1f, 1f, 1f, 1f, 0.5f, 0.1f,
        1f, -1f, 1f, 0.5f, 0.5f, 0.1f,  //ściana 3 (X -1 Z S T  Tid)
        -1f, -1f, -1f, 0f, 0.5f, 0.1f,
        1f, -1f, -1f, 0.5f, 0.5f, 0.1f,
        -1f, -1f, 1f, 0f, 1f, 0.1f,
        1f, -1f, -1f, 0.5f, 0.5f, 0.1f,
        1f, -1f, 1f, 0.5f, 1f, 0.1f,
        -1f, -1f, 1f, 0f, 1f, 0.1f,  //ściana 6 (X +1 Z S T  Tid)
        -1f, 1f, -1f, 0f, 0.5f, 0.1f,
        -1f, 1f, 1f, 0f, 1f, 0.1f,
        1f, 1f, -1f, 0.5f, 0.5f, 0.1f,
        1f, 1f, -1f, 0.5f, 0.5f, 0.1f,
        -1f, 1f, 1f, 0f, 1f, 0.1f,
        1f, 1f, 1f, 0.5f, 1f, 0.1f
    )

    private val deskCoords = floatArrayOf( // BLAT
        2f, 1f, -1f, 0f, 0f, 1.1f,
        2f, 1f, 1f, 0f, 1f, 1.1f,
        5f, 1f, -1f, 1f, 0f, 1.1f,
        5f, 1f, -1f, 1f, 0f, 1.1f,
        2f, 1f, 1f, 0f, 1f, 1.1f,
        5f, 1f, 1f, 1f, 1f, 1.1f,  // NOGA
        3f, -1f, 0f, 0f, 0f, 1.1f,
        3f, 1f, 0f, 0f, 0.5f, 1.1f,
        4f, -1f, 0f, 1f, 0f, 1.1f,
        3f, 1f, 0f, 0f, 0.5f, 1.1f,
        4f, 1f, 0f, 1f, 0.5f, 1.1f,
        4f, -1f, 0f, 1f, 0f, 1.1f
    )

    // Uchwyt do programu shadera
    private var renderProgram: Int

    // Uchwyty do atrybutów i uniform shadera
    private var vPositionHandle = 0
    private var mvpHandle = 0
    private var mTextureUniformHandle = 0
    private var vTexCoordinateHandle = 0

    // Uchwyty do tekstur
    private val mTextureDataHandle: IntArray?
    private var vTexIdHandle = 0

    init {
        // Wczytanie z zasobów plików graficznych i utworzenie tekstur
        mTextureDataHandle = IntArray(4)
        val bitmap = arrayOfNulls<Bitmap>(4)
        bitmap[0] = BitmapFactory.decodeResource(ctx.resources, R.drawable.cube)
        bitmap[1] = BitmapFactory.decodeResource(ctx.resources, R.drawable.wood1)
        bitmap[2] = BitmapFactory.decodeResource(ctx.resources, R.drawable.b2)
        bitmap[3] = BitmapFactory.decodeResource(ctx.resources, R.drawable.b3)
        GLES20.glGenTextures(4, mTextureDataHandle, 0)
        for (i in 0..3) {
            // Powiązanie z teksturą w OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[i])
            // Ustawienie filtrowania i krawędzi (trzeba ustawić dla każdej tekstury)
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )
            // Wczytanie danych bitmapy do tekstury
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap[i], 0)
            // Wyczyszczenie bitmapy po utworzeniu tekstury
            bitmap[i]?.recycle()
        }
        // Utworzenie buforów ze współrzędnymi geometrycznymi punktów
        val buf = ByteBuffer.allocateDirect(NODE_SIZE * 3 * maxTriangles)
        buf.order(ByteOrder.nativeOrder())
        buffer = buf.asFloatBuffer()
        buffer.put(cubeCoords)
        buffer.put(deskCoords)
        buffer.flip()
        // Utworzenie programu shadera (vertex + fragment)
        renderProgram = OShader.createShader()
    }

    fun draw(mvpMatrix: FloatArray?) {
        // Wybranie shadera
        GLES20.glUseProgram(renderProgram)

        // PARAMETRY UNIFORM:
        // MACIERZ MVP
        mvpHandle = GLES20.glGetUniformLocation(renderProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        //DOSTĘPNE TEKSTURY
        for (i in mTextureDataHandle!!.indices) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i + 1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle[i])
        }
        mTextureUniformHandle = GLES20.glGetUniformLocation(renderProgram, "s_texture")
        GLES20.glUniform1iv(mTextureUniformHandle, 4, mTextureDataHandle, 0)

        //PARAMETRY ATTRIBUTE
        //Współrzędne
        vPositionHandle = GLES20.glGetAttribLocation(renderProgram, "a_Position")
        GLES20.glEnableVertexAttribArray(vPositionHandle)
        //Współrzędne tekstur
        vTexCoordinateHandle = GLES20.glGetAttribLocation(renderProgram, "a_Texture")
        GLES20.glEnableVertexAttribArray(vTexCoordinateHandle)
        //Indeksy tekstur
        vTexIdHandle = GLES20.glGetAttribLocation(renderProgram, "a_Texture_id")
        GLES20.glEnableVertexAttribArray(vTexIdHandle)

        //Ustawienie wskaźnika bufora na współrzędne geometr. (trzy, od pierwszej)
        buffer.position(0)
        //Przekazanie współrzędnych geometrycznych do shadera
        GLES20.glVertexAttribPointer(
            vPositionHandle, 3, GLES20.GL_FLOAT,
            false, NODE_SIZE, buffer
        )

        //Ustawienie wskaźnika bufora na ID tekstury (szósta współrzędna)
        buffer.position(5)
        //Przekazanie współrzędnych ID tekstury do shadera
        GLES20.glVertexAttribPointer(
            vTexIdHandle, 1, GLES20.GL_FLOAT,
            false, NODE_SIZE, buffer
        )

        //Ustawienie wskaźnika bufora na współrzędne tekstury (dwie, od czwartej)
        buffer.position(3)
        //Przekazanie współrzędnych tekstury do shadera
        GLES20.glVertexAttribPointer(
            vTexCoordinateHandle, 2, GLES20.GL_FLOAT,
            false, NODE_SIZE, buffer
        )

        //NARYSOWANIE OBIEKTÓW (jedno wywołanie DrawArrays()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3 * maxTriangles)

        //Wyłączenie możliwości modyfikacji atrybutów
        for (i in mTextureDataHandle.indices) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i + 1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
        GLES20.glDisableVertexAttribArray(vTexIdHandle)
        GLES20.glDisableVertexAttribArray(vTexCoordinateHandle)
        GLES20.glDisableVertexAttribArray(vPositionHandle)
        GLES20.glUseProgram(0)
    }

}
