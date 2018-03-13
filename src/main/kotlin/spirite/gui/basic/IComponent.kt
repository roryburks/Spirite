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

    data class MouseEvent(val point: UIPoint)
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

