package spirite.gui.basic

import spirite.gui.UIPoint
import javax.swing.JComponent

interface IComponent {
    fun redraw()
    var enabled : Boolean
    val width: Int
    val height: Int

    var onResize : (() -> Unit)?
    var onHidden : (() -> Unit)?
    var onShown : (() -> Unit)?
    var onMoved : (() -> Unit)?

    enum class MouseButton {
        LEFT, RIGHT, CENTER, UNKNOWN
    }
    data class MouseEvent(
            val point: UIPoint,
            val button: MouseButton,
            val modifierMask : Int)
    {

        val holdingShift get() = (modifierMask and shiftMask) == shiftMask
        val holdingCtrl get() = (modifierMask and ctrlMask) == ctrlMask
        val holdingAlt get() = (modifierMask and altMask) == altMask

        companion object {
            val shiftMask = 0b1
            val ctrlMask = 0b10
            val altMask = 0b100

            fun toMask( holdingShift: Boolean, holdingCtrl: Boolean, holdingAlt: Boolean) : Int =
                    if( holdingShift) shiftMask else 0 +
                    if( holdingCtrl) ctrlMask else 0 +
                    if( holdingAlt) altMask else 0
        }
    }
    var onMouseClick : ((MouseEvent) -> Unit)?
    var onMousePress : ((MouseEvent) -> Unit)?
    var onMouseRelease : ((MouseEvent) -> Unit)?
    var onMouseEnter : ((MouseEvent) -> Unit)?
    var onMouseExit : ((MouseEvent) -> Unit)?
    var onMouseMove : ((MouseEvent) -> Unit)?
    var onMouseDrag : ((MouseEvent) -> Unit)?

    data class MouseWheelEvent(
            val point: UIPoint,
            val moveAmount : Int)
    var onMouseWheelMoved : ((MouseWheelEvent)->Unit)?
}

class Invokable<T>() {
    constructor(invoker : () -> T) : this() {
        this.invoker = invoker
    }
    lateinit var invoker : ()-> T
}

