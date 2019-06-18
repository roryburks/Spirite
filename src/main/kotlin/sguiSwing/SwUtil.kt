package sguiSwing

import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.KeyStroke

object SwUtil {
    fun buildAction( action: (ActionEvent)->Unit) = object: AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            action.invoke(e)
        }
    }

    fun buildActionMap(component: JComponent, actionMap: Map<KeyStroke, Action>) {
        for ((key, value) in actionMap) {
            val id = key.toString()
            component.inputMap.put(key, id)
            component.actionMap.put(id, value)
        }
    }

    /***
     * Draws the string centered in the given RectShape (using the font already
     * set up in the Graphics)
     */
    fun drawStringCenter(g: Graphics, text: String, rect: Rectangle) {
        val fm = g.fontMetrics
        val dx = (rect.width - fm.stringWidth(text)) / 2
        val dy = (rect.height - fm.height) / 2 + fm.ascent
        g.drawString(text, dx, dy)
    }
}