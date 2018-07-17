package spirite.gui.components.major.work

import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.awt.GLJPanel
import spirite.base.graphics.gl.GLGraphicsContext
import spirite.base.pen.Penner
import spirite.hybrid.Hybrid
import spirite.pc.JOGL.JOGL
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.gui.basic.ISwComponent
import spirite.pc.gui.basic.SwComponent

class JOGLWorkAreaPanel
private constructor(
        context: WorkSection,
        private val penner: Penner,
        val canvas: GLJPanel)
    : WorkArea(context), ISwComponent by SwComponent(canvas)
{
    constructor( context: WorkSection, penner: Penner) : this(
            context,
            penner,
            GLJPanel( GLCapabilities(GLProfile.getDefault()))
    )

    override val scomponent: ISwComponent get() = this

    init {
        Hybrid.timing.createTimer(50, true){redraw()}

        onMouseMove = {
            penner.holdingAlt = it.holdingAlt
            penner.holdingCtrl = it.holdingCtrl
            penner.holdingShift = it.holdingShift
            penner.rawUpdateX(it.point.x)
            penner.rawUpdateY(it.point.y)
        }
        onMouseDrag = onMouseMove
        onMousePress = {
            penner.holdingAlt = it.holdingAlt
            penner.holdingCtrl = it.holdingCtrl
            penner.holdingShift = it.holdingShift
            penner.penDown(it.button)
        }
        onMouseRelease = {
            penner.holdingAlt = it.holdingAlt
            penner.holdingCtrl = it.holdingCtrl
            penner.holdingShift = it.holdingShift
            penner.penUp(it.button)
        }

        canvas.addGLEventListener(object : GLEventListener {
            override fun reshape(drawable: GLAutoDrawable?, x: Int, y: Int, width: Int, height: Int) {}
            override fun dispose(drawable: GLAutoDrawable?) {}

            override fun display(drawable: GLAutoDrawable) {

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