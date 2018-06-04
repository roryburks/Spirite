package spirite.pc.gui.basic

import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.gui.SUIPoint
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder
import spirite.gui.components.basic.IComponent.BasicBorder.*
import spirite.gui.components.basic.IComponent.BasicCursor
import spirite.gui.components.basic.events.MouseEvent.MouseButton.*
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseWheelEvent
import spirite.gui.components.basic.Invokable
import spirite.gui.resources.Skin
import spirite.pc.gui.SColor
import spirite.pc.gui.jcolor
import spirite.pc.gui.scolor
import java.awt.Cursor
import java.awt.event.InputEvent.*
import java.awt.event.MouseWheelListener
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.border.BevelBorder
import java.awt.event.MouseEvent as JMouseEvent

interface ISwComponent : IComponent {
    override val component : JComponent
}

val IComponent.jcomponent get() = this.component as JComponent

abstract class ASwComponent : ISwComponent {
    override fun redraw() {component.repaint()}

    override var enabled: Boolean
        get() = component.isEnabled
        set(value) {component.isEnabled = value}
    override val height: Int get() = component.height
    override val width: Int get() = component.width

    override val x : Int get() = component.x
    override val y : Int get() = component.y

    override var background: SColor
        get() = component.background.scolor
        set(value) {component.background = value.jcolor}
    override var foreground: SColor
        get() = component.foreground.scolor
        set(value) {component.foreground = value.jcolor}
    override var opaque: Boolean
        get() = component.isOpaque
        set(value) {component.isOpaque = value}

    override fun setBasicCursor(cursor: BasicCursor) {
        component.cursor = Cursor.getPredefinedCursor(when( cursor) {
            IComponent.BasicCursor.CROSSHAIR -> Cursor.CROSSHAIR_CURSOR
            IComponent.BasicCursor.DEFAULT -> Cursor.DEFAULT_CURSOR
            IComponent.BasicCursor.E_RESIZE -> Cursor.E_RESIZE_CURSOR
            IComponent.BasicCursor.HAND -> Cursor.HAND_CURSOR
            IComponent.BasicCursor.MOVE -> Cursor.MOVE_CURSOR
            IComponent.BasicCursor.N_RESIZE -> Cursor.N_RESIZE_CURSOR
            IComponent.BasicCursor.NE_RESIZE -> Cursor.NE_RESIZE_CURSOR
            IComponent.BasicCursor.NW_RESIZE -> Cursor.NW_RESIZE_CURSOR
            IComponent.BasicCursor.S_RESIZE -> Cursor.S_RESIZE_CURSOR
            IComponent.BasicCursor.SE_RESIZE -> Cursor.SE_RESIZE_CURSOR
            IComponent.BasicCursor.SW_RESIZE -> Cursor.SW_RESIZE_CURSOR
            IComponent.BasicCursor.TEXT -> Cursor.TEXT_CURSOR
            IComponent.BasicCursor.W_RESIZE -> Cursor.W_RESIZE_CURSOR
            IComponent.BasicCursor.WAIT -> Cursor.WAIT_CURSOR
        })
    }

    override fun setBasicBorder(border: BasicBorder?) {
        component.border = when( border) {
            null -> null
            BASIC -> BorderFactory.createLineBorder( Skin.Global.BgDark.jcolor)
            BEVELED_LOWERED -> BorderFactory.createBevelBorder(BevelBorder.LOWERED, Skin.BevelBorder.Med.jcolor, Skin.BevelBorder.Dark.jcolor)
            BEVELED_RAISED -> BorderFactory.createBevelBorder(BevelBorder.RAISED, Skin.BevelBorder.Med.jcolor, Skin.BevelBorder.Dark.jcolor)
        }
    }

    // region ComponentListener
    override var onResize: (() -> Unit)?
        get() = componentListener.onResize
        set(value) { componentListener.onResize = value}
    override var onHidden: (() -> Unit)?
        get() = componentListener.onHidden
        set(value) { componentListener.onHidden = value}
    override var onShown: (() -> Unit)?
        get() = componentListener.onShown
        set(value) { componentListener.onShown = value}
    override var onMoved: (() -> Unit)?
        get() = componentListener.onMoved
        set(value) { componentListener.onMoved = value}

    private class JSComponentListener(
            var onResize : (() -> Unit)? = null,
            var onHidden : (() -> Unit)? = null,
            var onShown : (() -> Unit)? = null,
            var onMoved : (() -> Unit)? = null) : java.awt.event.ComponentListener
    {
        override fun componentMoved(e: java.awt.event.ComponentEvent?) {onMoved?.invoke()}
        override fun componentResized(e: java.awt.event.ComponentEvent?) {onResize?.invoke()}
        override fun componentHidden(e: java.awt.event.ComponentEvent?) {onHidden?.invoke()}
        override fun componentShown(e: java.awt.event.ComponentEvent?) {onShown?.invoke()}
    }
    private val componentListener by lazy {  JSComponentListener().apply { component.addComponentListener(this)}}
    // endregion

    // region MouseListener
    override var onMouseClick: ((MouseEvent) -> Unit)?
        get() = mouseListener.onMouseClick
        set(value) {mouseListener.onMouseClick = value}
    override var onMousePress: ((MouseEvent) -> Unit)?
        get() = mouseListener.onMousePress
        set(value) {mouseListener.onMousePress = value}
    override var onMouseRelease: ((MouseEvent) -> Unit)?
        get() = mouseListener.onMouseRelease
        set(value) {mouseListener.onMouseRelease = value}
    override var onMouseEnter: ((MouseEvent) -> Unit)?
        get() = mouseListener.onMouseEnter
        set(value) {mouseListener.onMouseEnter = value}
    override var onMouseExit: ((MouseEvent) -> Unit)?
        get() = mouseListener.onMouseExit
        set(value) {mouseListener.onMouseExit = value}
    override var onMouseMove: ((MouseEvent) -> Unit)?
        get() = mouseListener.onMouseMove
        set(value) {mouseListener.onMouseMove = value}
    override var onMouseDrag: ((MouseEvent) -> Unit)?
        get() = mouseListener.onMouseDrag
        set(value) {mouseListener.onMouseDrag = value}

    private val mouseListener by lazy {
        JSMouseListener().apply {
            component.addMouseListener(this)
            component.addMouseMotionListener(this)
        }
    }
    private class JSMouseListener(
            var onMouseClick : ((MouseEvent) -> Unit)? = null,
            var onMousePress : ((MouseEvent) -> Unit)? = null,
            var onMouseRelease : ((MouseEvent) -> Unit)? = null,
            var onMouseEnter : ((MouseEvent) -> Unit)? = null,
            var onMouseExit : ((MouseEvent) -> Unit)? = null,
            var onMouseMove : ((MouseEvent) -> Unit)? = null,
            var onMouseDrag : ((MouseEvent) -> Unit)? = null)
        :java.awt.event.MouseListener, java.awt.event.MouseMotionListener
    {
        var startX = 0
        var startY = 0

        fun convert( e: JMouseEvent) : MouseEvent {
            val scomp = SwComponent(e.component as JComponent)
            val smask = e.modifiersEx
            val mask = MouseEvent.toMask(
                    (smask and SHIFT_DOWN_MASK) == SHIFT_DOWN_MASK,
                    (smask and CTRL_DOWN_MASK) == CTRL_DOWN_MASK,
                    (smask and ALT_DOWN_MASK) == ALT_DOWN_MASK)
            return MouseEvent(
                    SUIPoint(e.x, e.y, scomp),
                    when (e.button) {
                        JMouseEvent.BUTTON1 -> LEFT
                        JMouseEvent.BUTTON2 -> CENTER
                        JMouseEvent.BUTTON3 -> RIGHT
                        else -> UNKNOWN
                    },
                    mask)
        }

        override fun mouseReleased(e: JMouseEvent) {
            onMouseRelease?.invoke( convert(e))
            if( e.component.bounds.contains(e.point) && MathUtil.distance(startX.f, startY.f, e.x.f, e.y.f) < 4)
                onMouseClick?.invoke(convert(e))
        }
        override fun mouseEntered(e: JMouseEvent) { onMouseEnter?.invoke( convert(e))}
        override fun mouseClicked(e: JMouseEvent) {onMouseClick?.invoke(convert(e))}
        override fun mouseExited(e: JMouseEvent) {onMouseExit?.invoke(convert(e))}
        override fun mousePressed(e: JMouseEvent) {
            startX = e.x
            startY = e.y
            onMousePress?.invoke(convert(e))
        }
        override fun mouseMoved(e: JMouseEvent) {onMouseMove?.invoke(convert(e))}
        override fun mouseDragged(e: JMouseEvent) { onMouseDrag?.invoke(convert(e))}
    }

    // endregion

    override var onMouseWheelMoved: ((MouseWheelEvent) -> Unit)?
        get() = mouseWheelListener.onWheelMove
        set(value) {mouseWheelListener.onWheelMove = value}
    private val mouseWheelListener by lazy { JSMouseWheelListener().apply { component.addMouseWheelListener( this) }}
    private class JSMouseWheelListener( var onWheelMove : ((MouseWheelEvent)-> Unit)? = null) : MouseWheelListener {
        fun convert( e: java.awt.event.MouseWheelEvent) : MouseWheelEvent {
            val scomp = SwComponent(e.component as JComponent)
            return MouseWheelEvent(SUIPoint(e.x, e.y, scomp), e.wheelRotation)
        }

        override fun mouseWheelMoved(e: java.awt.event.MouseWheelEvent) {onWheelMove?.invoke(convert(e))}
    }
}

class SwComponentIndirect(cGetter : Invokable<JComponent>) : ASwComponent() {
    override val component: JComponent by lazy { cGetter.invoker.invoke() }

}

class SwComponent(override val component: JComponent) : ASwComponent()

