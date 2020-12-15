package rbJvm.glow.jogl

import com.jogamp.opengl.*
import rb.glow.gl.IGL

object JOGLProvider {
    private val drawable: GLOffscreenAutoDrawable
    private val _gl : GL

    var gl2 : GL2? = null

    // Essentially, the workspace needs to be changed at two key times:
    //  1) When a JOGL Panel starts drawing (changed to use that panel's workspace)
    //  2) The first draw done AFTER the JOGL Panel is out of workspace (changed to the offscreen Drawable's workspace)
    // There is probably a more strategic process for this than wrapping the gl2 in JOGL every time something needs to
    //  make a GL call, but really this wrapping process should be no larger than a normal variable getter as JOGL is
    //  just a single variable and a ton of methods.
    val gl : IGL
        get() {
            val gl2 = gl2 ?: _gl.gL2

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

        _gl = gl ?: throw NullPointerException("No GL Loaded")

        _gl.gl.context.makeCurrent()
    }
}