package spirite.gui.views.work

import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLJPanel
import jpen.*
import jpen.PButton.Type.*
import jpen.event.PenListener
import jpen.owner.multiAwt.AwtPenToolkit
import rb.vectrix.mathUtil.round
import spirite.base.graphics.gl.GLGraphicsContext
import spirite.base.pen.Penner
import spirite.gui.components.basic.events.MouseEvent.MouseButton
import spirite.hybrid.Hybrid
import spirite.pc.JOGL.JOGLProvider
import spirite.pc.gui.basic.ISwComponent
import spirite.pc.gui.basic.SwComponent
import java.awt.event.MouseAdapter
import javax.swing.SwingUtilities

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
        this.canvas.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: java.awt.event.MouseEvent?) {
                requestFocus()
                super.mousePressed(e)
            }
        })

        canvas.skipGLOrientationVerticalFlip = true
        Hybrid.timing.createTimer(50, true){redraw()}

        fun MButtonFromPButton( pbutton: PButton) = when(pbutton.type) {
            LEFT -> MouseButton.LEFT
            RIGHT -> MouseButton.RIGHT
            CENTER -> MouseButton.CENTER
            else -> null
        }

        AwtPenToolkit.addPenListener(canvas, object: PenListener {
            override fun penKindEvent(evt: PKindEvent) {
                SwingUtilities.invokeLater {
                    // TODO: Switch between active sets
                    when (evt.kind.type) {
                        PKind.Type.CURSOR -> {
                        }
                        PKind.Type.STYLUS -> {
                        }
                        PKind.Type.ERASER -> {
                        }
                        else -> {
                        }
                    }
                }
            }

            override fun penButtonEvent(evt: PButtonEvent) {
                val button = MButtonFromPButton(evt.button) ?: return

                SwingUtilities.invokeLater {
                    JOGLProvider.context.makeCurrent()
                    when (evt.button.value) {
                        true -> penner.penDown(button)
                        false -> penner.penUp(button)
                    }
                    JOGLProvider.context.release()
                }
            }

            override fun penTock(p0: Long) {}

            override fun penLevelEvent(evt: PLevelEvent) {
                evt.levels.forEach { level ->
                    when( level.type ) {
                        PLevel.Type.X -> penner.rawUpdateX(level.value.round)
                        PLevel.Type.Y -> penner.rawUpdateY(level.value.round)
                        PLevel.Type.PRESSURE -> penner.rawUpdatePressure(level.value)
                        else -> {}
                    }
                }
            }

            override fun penScrollEvent(p0: PScrollEvent) {}

        })

        val mouseAdapter = object : MouseAdapter() {
            fun update(e: java.awt.event.MouseEvent) {
                penner.holdingAlt = e.isAltDown
                penner.holdingCtrl = e.isControlDown
                penner.holdingShift = Hybrid.keypressSystem.holdingSpace

            }

            override fun mouseMoved(e: java.awt.event.MouseEvent) = update(e)
            override fun mouseDragged(e: java.awt.event.MouseEvent)  = update(e)
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                update(e)
                canvas.requestFocus()
            }
            override fun mouseReleased(e: java.awt.event.MouseEvent) = update(e)
        }

        canvas.addMouseMotionListener(mouseAdapter)
        canvas.addMouseListener(mouseAdapter)

        canvas.addGLEventListener(object : GLEventListener {
            override fun reshape(drawable: GLAutoDrawable?, x: Int, y: Int, width: Int, height: Int) {}
            override fun dispose(drawable: GLAutoDrawable?) {}

            override fun display(drawable: GLAutoDrawable) {

                val w = drawable.surfaceWidth
                val h = drawable.surfaceHeight

                val gle = Hybrid.gle
                val glgc = GLGraphicsContext(w, h, false, gle, true)

                JOGLProvider.gl2 = drawable.gl.gL2
                gle.setTarget(null)
                glgc.clear()

                val gl = gle.gl
                gl.viewport(0, 0, w, h)

                drawWork(glgc)
                JOGLProvider.gl2 = null
            }

            override fun init(drawable: GLAutoDrawable) {
                // Disassociate default workspace and assosciate the workspace from the GLEngine
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