package spirite.gui.components.major.work

import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLJPanel
import spirite.base.graphics.gl.GLGraphicsContext
import spirite.hybrid.Hybrid
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.gui.basic.ISwComponent
import spirite.pc.gui.basic.SwComponent

class JOGLWorkArea
private constructor(context: WorkSection, val canvas: GLJPanel)
    : WorkArea(context), ISwComponent by SwComponent(canvas)
{
    constructor( context: WorkSection) : this( context,GLJPanel( GLCapabilities(GLProfile.getDefault())))

    override val scomponent: ISwComponent get() = this

    init {
        canvas.addGLEventListener(object : GLEventListener {
            override fun reshape(drawable: GLAutoDrawable?, x: Int, y: Int, width: Int, height: Int) {}
            override fun dispose(drawable: GLAutoDrawable?) {}

            override fun display(drawable: GLAutoDrawable) {
                drawable.context.makeCurrent()

                val w = drawable.surfaceWidth
                val h = drawable.surfaceHeight

                val gle = Hybrid.gle
                val glgc = GLGraphicsContext(w, h, true, gle, false)

                JOGLProvider.gl2 = drawable.gl.gL2
                gle.setTarget(null)

                val gl = gle.getGl()
                gl.viewport(0, 0, w, h)

                drawWork(glgc)
                JOGLProvider.gl2 = null
            }

            override fun init(drawable: GLAutoDrawable) {
                // Disassociate default context and assosciate the context from the GLEngine
                //	(so they can share resources)
                val primaryContext = JOGLProvider.context

                val unusedDefaultContext = drawable.context
                unusedDefaultContext.makeCurrent()
                drawable.setContext( null, true)

                val subContext = drawable.createContext( primaryContext)
                subContext.makeCurrent()
                drawable.setContext(subContext, true)
            }
        })
    }
}