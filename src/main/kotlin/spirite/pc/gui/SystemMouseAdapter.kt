package spirite.pc.gui

import spirite.gui.SUIPoint
import spirite.gui.components.basic.events.MouseEvent.MouseButton.*
import spirite.gui.components.basic.events.MouseEvent.MouseEventType
import spirite.gui.components.basic.events.MouseEvent.MouseEventType.*
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SwComponent
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import javax.swing.JComponent
import javax.swing.SwingUtilities

fun JComponent.adaptMouseSystem()
{
    val adapter = SystemMouseAdapter(this)
    addMouseListener(adapter)
    addMouseMotionListener(adapter)
}

class SystemMouseAdapter(val comp : JComponent) : MouseListener, MouseMotionListener
{
    override fun mouseReleased(e: MouseEvent) {
        val evt = convert(e, RELEASED)
        Hybrid.mouseSystem.broadcastMouseEvent(evt, SwingUtilities.getRoot(e.component))
    }

    override fun mouseEntered(e: MouseEvent) {
        val evt = convert(e, ENTERED)
        Hybrid.mouseSystem.broadcastMouseEvent(evt, SwingUtilities.getRoot(e.component))
    }

    override fun mouseClicked(e: MouseEvent) {
        val evt = convert(e, CLICKED)
        Hybrid.mouseSystem.broadcastMouseEvent(evt, SwingUtilities.getRoot(e.component))
    }

    override fun mouseExited(e: MouseEvent) {
        val evt = convert(e, EXITED)
        Hybrid.mouseSystem.broadcastMouseEvent(evt, SwingUtilities.getRoot(e.component))
    }

    override fun mousePressed(e: MouseEvent) {
        val evt = convert(e, PRESSED)
        Hybrid.mouseSystem.broadcastMouseEvent(evt, SwingUtilities.getRoot(e.component))
    }

    override fun mouseMoved(e: MouseEvent) {
        val evt = convert(e, MOVED)
        Hybrid.mouseSystem.broadcastMouseEvent(evt, SwingUtilities.getRoot(e.component))
    }

    override fun mouseDragged(e: MouseEvent) {
        val evt = convert(e, DRAGGED)
        Hybrid.mouseSystem.broadcastMouseEvent(evt, SwingUtilities.getRoot(e.component))
    }

    fun convert(e: MouseEvent, type: MouseEventType) : spirite.gui.components.basic.events.MouseEvent {
        val scomp = SwComponent(e.component as Component)
        val smask = e.modifiersEx
        val mask = spirite.gui.components.basic.events.MouseEvent.toMask(
                (smask and InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK,
                (smask and InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK,
                (smask and InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK)

        return spirite.gui.components.basic.events.MouseEvent(
                SUIPoint(e.x, e.y, scomp.component),
                when (e.button) {
                    MouseEvent.BUTTON1 -> LEFT
                    MouseEvent.BUTTON2 -> CENTER
                    MouseEvent.BUTTON3 -> RIGHT
                    else -> UNKNOWN
                },
                mask,
                type)
    }
}