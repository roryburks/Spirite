package sgui.components

import rb.glow.color.SColor
import rb.vectrix.shapes.RectI
import sgui.UIPoint
import sgui.components.events.MouseEvent
import sgui.components.events.MouseWheelEvent

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
    val bounds: RectI get() = RectI(x, y, width, height)
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
    fun setColoredBorder(color: SColor, width: Int = 1)

    val onResize : EventStack<Unit>
    val onHidden : EventStack<Unit>
    val onShown : EventStack<Unit>
    val onMoved : EventStack<Unit>

    val onMouseClick : EventStack<MouseEvent>
    val onMousePress : EventStack<MouseEvent>
    val onMouseRelease : EventStack<MouseEvent>
    val onMouseEnter : EventStack<MouseEvent>
    val onMouseExit : EventStack<MouseEvent>
    val onMouseMove : EventStack<MouseEvent>
    val onMouseDrag : EventStack<MouseEvent>

    open class EventStack<Event> {
        val triggers : List<(Event)->Unit> get() = _triggers ?: emptyList()
        private var _triggers : MutableList<(Event)->Unit>? = null ; private set

        operator fun plusAssign( onEvent: (Event)->Unit) {
            if( _triggers == null) _triggers = mutableListOf()
            _triggers!!.add(onEvent)
        }
        fun clear() = _triggers?.clear()
        fun remove( onEvent: (Event) -> Unit) = _triggers?.remove(onEvent)
    }

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

