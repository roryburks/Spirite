package spirite.gui.basic

import spirite.gui.basic.events.MouseEvent
import spirite.gui.basic.events.MouseWheelEvent

interface IComponent {
    val component: Any  // This should be the internal root component for things that might need it

    fun redraw()
    var enabled : Boolean
    val width: Int
    val height: Int


    enum class BasicCursor {
        CROSSHAIR, DEFAULT, E_RESIZE, HAND, MOVE, N_RESIZE, NE_RESIZE, NW_RESIZE, S_RESIZE, SE_RESIZE, SW_RESIZE, TEXT, W_RESIZE, WAIT
    }
    fun setBasicCursor( cursor: BasicCursor)

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
}

class Invokable<T>() {
    constructor(invoker : () -> T) : this() {
        this.invoker = invoker
    }
    lateinit var invoker : ()-> T
}

