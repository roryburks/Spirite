package sguiSwing.mouseSystem

import java.awt.event.MouseEvent
import java.awt.event.MouseListener

class SimpleMouseListener( private val onClick: (MouseEvent)->Unit) : MouseListener {
    /* TODO: Might make this more lenient of a click, as tablet pens tend to move a little while clicking, whereas
      the Swing Click listener doesn't activate if it's moved even a single pixel */
    override fun mousePressed(e: MouseEvent?) {}
    override fun mouseReleased(e: MouseEvent?) {}

    override fun mouseEntered(e: MouseEvent?) {}

    override fun mouseClicked(e: MouseEvent) {
        onClick.invoke(e)
    }

    override fun mouseExited(e: MouseEvent?) {}


}