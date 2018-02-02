package spirite.pc.JOGL

import com.jogamp.opengl.*

object JOGLProvider {
    val drawable: GLOffscreenAutoDrawable
    val gl : GL

    init {
        val profile = GLProfile.getDefault()
        val factory = GLDrawableFactory.getFactory(profile)
        val caps = GLCapabilities(profile)
        caps.hardwareAccelerated = true
        caps.doubleBuffered = false
        caps.alphaBits = 8
        caps.redBits = 8
        caps.blueBits = 8
        caps.greenBits = 8
        caps.isOnscreen = false

        drawable = factory.createOffscreenAutoDrawable(
                factory.defaultDevice,
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
        this.gl = gl ?: throw NullPointerException("No GL Loaded")
    }
}