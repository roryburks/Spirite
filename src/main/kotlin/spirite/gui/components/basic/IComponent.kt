package spirite.gui.components.basic

import spirite.base.util.linear.Rect
import spirite.gui.UIPoint
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseWheelEvent
import spirite.pc.gui.SColor

interface IComponent {
    val component: Any  // This should be the internal root component for things that might need it

    fun redraw()
    var enabled : Boolean
    val width: Int
    val height: Int
    val x: Int
    val y: Int
    val bounds: Rect get() = Rect(x, y, width, height)
    val topLeft : UIPoint
    val bottomRight: UIPoint

    var background : SColor
    var foreground : SColor
    var opaque : Boolean


    enum class BasicCursor {
        CROSSHAIR, DEFAULT, E_RESIZE, HAND, MOVE, N_RESIZE, NE_RESIZE, NW_RESIZE, S_RESIZE, SE_RESIZE, SW_RESIZE, TEXT, W_RESIZE, WAIT
    }
    fun setBasicCursor( cursor: BasicCursor)

    enum class BasicBorder {
        BEVELED_LOWERED, BEVELED_RAISED, BASIC
    }
    fun setBasicBorder( border: BasicBorder?)

    var onResize : (() -> Unit)?
    var onHidden : (() -> Unit)?
    var onShown : (() -> Unit)?
    var onMoved : (() -> Unit)?

    var onMouseClick : ((MouseEvent) -> Unit)?
    var onMousePress : ((MouseEvent) -> Unit)?
    var onMouseRelease : ((MouseEvent) -> Unit)?
    var onMouseEnter : ((MouseEvent) -> Unit)?
    var onMouseExit : ((MouseEvent) -> Unit)?
    var onMouseMove : ((MouseEvent) -> Unit)?
    var onMouseDrag : ((MouseEvent) -> Unit)?

    var onMouseWheelMoved : ((MouseWheelEvent)->Unit)?

    fun addEventOnKeypress( keycode: Int,  modifiers: Int, action: () -> Unit)
    fun requestFocus()
}

class Invokable<T>() {
    constructor(invoker : () -> T) : this() {
        this.invoker = invoker
    }
    lateinit var invoker : ()-> T
}

