package spirite.pc.JOGL

import com.jogamp.opengl.*
import spirite.base.graphics.gl.IGL
import spirite.gui.Bindable

object JOGLProvider {
    private val drawable: GLOffscreenAutoDrawable
    private val _gl : GL

    var gl2 : GL2? = null

    val gl : IGL get() {
        val gl2 = gl2 ?: this._gl.gL2
        if( !gl2.context.isCurrent)
            gl2.context.makeCurrent()
        return JOGL(gl2)
    }



    val context : GLContext get() = drawable.context

    init {
        val profile = GLProfile.getDefault()
        val fact = GLDrawableFactory.getFactory(profile)
        val caps = GLCapabilities(profile)
        caps.hardwareAccelerated = true
        caps.doubleBuffered = false
        caps.alphaBits = 8
        caps.redBits = 8
        caps.blueBits = 8
        caps.greenBits = 8
        caps.isOnscreen = false

        drawable = fact.createOffscreenAutoDrawable(
                fact.defaultDevice,
                caps,
                DefaultGLCapabilitiesChooser(),
                1, 1)

        var exception : Exception? = null
        var gl : GL? = null

        drawable.addGLEventListener( object :GLEventListener {
            override fun reshape(p0: GLAutoDrawable?, p1: Int, p2: Int, p3: Int, p4: Int) {}
            override fun display(p0: GLAutoDrawable?) {}
            override fun dispose(p0: GLAutoDrawable?) {}
            override fun init(gad: GLAutoDrawable?) {
                try {
                    gl = gad?.gl
                }catch( e : Exception) {
                    exception = e
                }
            }
        })

        drawable.display()

        if( exception!= null)
            throw Exception(exception)

        this._gl = gl ?: throw NullPointerException("No GL Loaded")

        this._gl.gl.context.makeCurrent()
    }
}