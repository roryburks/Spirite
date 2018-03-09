package spirite.gui.major.work

import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLJPanel
import spirite.base.graphics.gl.GLGraphicsContext
import spirite.base.util.Colors
import spirite.gui.basic.ISComponent
import spirite.gui.basic.Invokable
import spirite.gui.basic.SComponent
import spirite.hybrid.Hybrid
import spirite.pc.JOGL.JOGLProvider
import javax.swing.JComponent

class GLWorkArea
private constructor(context: WorkSection, invokable: Invokable<JComponent>)
    : WorkArea(context), ISComponent by SComponent(invokable)
{
    init {invokable.invoker = {canvas}}
    constructor( context: WorkSection) : this( context,Invokable())


    override val scomponent: ISComponent get() = this


    val canvas = GLJPanel( GLCapabilities(GLProfile.getDefault()))

    init {
        canvas.addGLEventListener(object : GLEventListener {
            override fun reshape(drawable: GLAutoDrawable?, x: Int, y: Int, width: Int, height: Int) {}
            override fun dispose(drawable: GLAutoDrawable?) {}

            override fun display(drawable: GLAutoDrawable) {
                drawable.context.makeCurrent()

                val w = drawable.surfaceWidth
                val h = drawable.surfaceHeight

                val gle = Hybrid.gle
                val glgc = GLGraphicsContext(w, h, true, gle)

                gle.setTarget(null)
                gle.gl.viewport(0, 0, w, h)

                drawWork(glgc)
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