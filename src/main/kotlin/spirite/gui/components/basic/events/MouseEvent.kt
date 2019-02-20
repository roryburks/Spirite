package spirite.gui.components.basic.events

import spirite.gui.UIPoint

data class MouseEvent
constructor(
        val point: UIPoint,
        val button: MouseButton,
        private val modifierMask : Int,
        val type : MouseEventType)
{
    enum class MouseEventType {
        RELEASED, ENTERED, CLICKED, EXITED, PRESSED, MOVED, DRAGGED,
    }
    enum class MouseButton {
        LEFT, RIGHT, CENTER, UNKNOWN
    }

    val holdingShift get() = (modifierMask and shiftMask) == shiftMask
    val holdingCtrl get() = (modifierMask and ctrlMask) == ctrlMask
    val holdingAlt get() = (modifierMask and altMask) == altMask

    companion object {
        val shiftMask = 0b1
        val ctrlMask = 0b10
        val altMask = 0b100

        fun toMask( holdingShift: Boolean, holdingCtrl: Boolean, holdingAlt: Boolean) : Int =
                (if( holdingShift) shiftMask else 0) or
                (if( holdingCtrl) ctrlMask else 0) or
                (if( holdingAlt) altMask else 0)
    }
}