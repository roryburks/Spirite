package demonstration

import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLJPanel
import java.nio.FloatBuffer
import javax.swing.JFrame
import javax.swing.UIManager
import javax.swing.WindowConstants

fun main( args: Array<String>) {
    UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName())

    val frame = JglpanelDemo()
    frame.pack()
    frame.setSize(100, 100)
    frame.isLocationByPlatform = true
    frame.isVisible = true
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
}

class JglpanelDemo : JFrame() {
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

        val offscreenDrawable = fact.createOffscreenAutoDrawable(fact.defaultDevice,caps, DefaultGLCapabilitiesChooser(),1, 1)
        offscreenDrawable.display()

        val gljPanel = GLJPanel()
        gljPanel.addGLEventListener(object : GLEventListener {
            override fun reshape(drawable: GLAutoDrawable?, x: Int, y: Int, width: Int, height: Int) {}

            override fun display(drawable: GLAutoDrawable) {
                drawable.gl.gL2.glClearBufferfv(GL2.GL_COLOR, 0, FloatBuffer.wrap(floatArrayOf(1f,0f,0f,1f)))
            }

            override fun init(drawable: GLAutoDrawable) {
                // Disassociate default workspace and assosciate the workspace from the GLEngine
                //	(so they can share resources)
                val primaryContext = offscreenDrawable.context

                val unusedDefaultContext = drawable.context
                unusedDefaultContext.makeCurrent()
                drawable.setContext( null, true)

                val subContext = drawable.createContext( primaryContext)
                subContext.makeCurrent()
                drawable.setContext(subContext, true)
            }

            override fun dispose(drawable: GLAutoDrawable?) {}
        })

        add(gljPanel)
    }
}