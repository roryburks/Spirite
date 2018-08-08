package spirite.gui.components.basic

import spirite.base.util.linear.Rect
import spirite.gui.UIPoint
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseWheelEvent
import spirite.pc.gui.SColor

interface IComponent {
    // Ref is a way for abstract objects to attach themselves to the UI, primarily so that they can stay in memory.
    // Consider the following situation:
    //  -Abstract game component ToolSettingSection contains an IComponent representing its ui component on screen.
    //  -ToolSettingSection also has model logic in it and has listeners attached to it, it wants these listeners to
    //      be weak so that ToolSettingSection can disappear without having to tell its MasterControl so that it doesn't
    //      have to worry about explicitly listening for its death in the Swing universe.
    //  -The JComponent has a reference to IComponent, so the visuals stay in memory as long as it's in the Swing system
    //  -But IComponent does not have any reference to ToolSettingSection, so the only references that still exist are
    //      the weak listening system which ages it out.
    // If IComponent had a reference to ToolSettingSection, however, then it would stay in memory as long as the JComponent
    //  exists within the Swing System and then age out whenever it's gone.
    //
    // Note: since implementations that ARE IComponents through delegation are contiguous in memory, this only applies to things
    var ref : Any?

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
    fun markAsPassThrough()

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

