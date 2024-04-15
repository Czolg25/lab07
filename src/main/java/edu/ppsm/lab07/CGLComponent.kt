package edu.ppsm.lab07

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.Volatile

class CGLComponent(val ctx: Context) : GLSurfaceView( ctx ) {

    inner class CRenderer : Renderer {
        // Referencja do obiektu sceny
        private var scene: CGLScene? = null

        // Parametry sterowane przez użytkownika - modyfikowane z innego wątku
        @Volatile
        var obrotX = 0.0f

        @Volatile
        var obrotY = 0.0f

        @Volatile
        var dist = 20.0f

        // Macierze: M, V, P oraz MVP
        private val mPMatrix = FloatArray(16)
        private val mVMatrix = FloatArray(16)
        private val mMMatrix = FloatArray(16)
        private val mMVPMatrix = FloatArray(16)

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            GLES20.glClearColor(0.9f,0.9f,0.6f,1.0f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_TEXTURE_2D)
            GLES20.glFrontFace(GLES20.GL_CW)
            scene = CGLScene(ctx)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            GLES20.glViewport(0,0,width,height)
            val ratio = width.toFloat() / height.toFloat()
            Matrix.frustumM(mPMatrix,0,-ratio,ratio,-1f,1f,5.0f,100.0f)
        }

        override fun onDrawFrame(gl: GL10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            Matrix.setLookAtM(mVMatrix,0,0f,0f,dist,0f,0f,0.0f,0f,1.0f,0.0f)
            Matrix.multiplyMM(mMVPMatrix,0,mPMatrix,0,mVMatrix,0)
            Matrix.setIdentityM(mMMatrix,0)
            Matrix.setRotateM(mMMatrix,0,obrotX,0f,1.0f,0f)
            Matrix.setRotateM(mMMatrix,0,obrotY,1.0f,0f,0f)
            Matrix.scaleM(mMMatrix,0,0.7f,0.7f,0.7f)

            Matrix.multiplyMM(mMVPMatrix,0,mMVPMatrix,0,mMMatrix,0)
            scene!!.draw(mMVPMatrix)
        }




    }

    private var renderer: CRenderer
    private val touchScaleFactor = 180.0f / 320
    private var prevX = 0f
    private var prevY = 0f

    init {
        //OpenGL ES 2.0
        setEGLContextClientVersion(2)
        renderer = CRenderer()
        setRenderer(renderer)
        // Renderowanie tylko gdy konieczne (oszczędność energii)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when(event.action){
            MotionEvent.ACTION_MOVE -> {
                if(y> 200){
                    val dx = x - prevX
                    val dy = y - prevY
                    renderer.obrotX += dx * touchScaleFactor
                    renderer.obrotY += dy * touchScaleFactor
                }else {
                    if(x - prevX > 0){
                        renderer.dist += 1.0f
                        if(renderer.dist > 40.0f) renderer.dist = 40.0f
                    }else if(x - prevX < 0){
                        renderer.dist -= 1.0f
                        if(renderer.dist < 10.0f) renderer.dist = 10.0f
                    }
                }
                requestRender()
            }
            MotionEvent.ACTION_UP -> { performClick()}
            else -> {}
        }

        prevX = x
        prevY = y
        return true
    }

    override fun performClick(): Boolean {
        prevX = 0f
        prevY = 0f
        return super.performClick()
    }
}
