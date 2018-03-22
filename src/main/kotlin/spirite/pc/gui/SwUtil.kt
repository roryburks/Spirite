package spirite.pc.gui

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
}