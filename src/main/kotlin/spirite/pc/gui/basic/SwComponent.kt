package spirite.pc.gui.basic

import spirite.gui.SUIPoint
import spirite.gui.UIPoint
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.*
import spirite.gui.components.basic.IComponent.BasicBorder.*
import spirite.gui.components.basic.Invokable
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseEvent.MouseButton.*
import spirite.gui.components.basic.events.MouseWheelEvent
import spirite.gui.resources.Skin
import spirite.pc.gui.SColor
import spirite.pc.gui.jcolor
import spirite.pc.gui.scolor
import java.awt.Component
import java.awt.Cursor
import java.awt.event.*
import java.awt.event.InputEvent.*
import java.lang.ref.WeakReference
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.border.BevelBorder
import java.awt.event.MouseEvent as JMouseEvent

interface ISwComponent : IComponent {
    override val component : Component
}

val IComponent.jcomponent get() = this.component as Component

abstract class ASwComponent : ISwComponent {
    override var ref: Any? = null
    override fun redraw() {component.repaint()}

    override var enabled: Boolean
        get() = component.isEnabled
        set(value) {component.isEnabled = value}
    override val height: Int get() = component.height
    override val width: Int get() = component.width

    override val x : Int get() = component.x
    override val y : Int get() = component.y

    override val topLeft: UIPoint get() = SUIPoint(x, y, this.component.parent)
    override val bottomRight: UIPoint get() = SUIPoint(x+width, y+height, this.component.parent)

    override var background: SColor
        get() = component.background.scolor
        set(value) {component.background = value.jcolor}
    override var foreground: SColor
        get() = component.foreground.scolor
        set(value) {component.foreground = value.jcolor}
    override var opaque: Boolean
        get() = component.isOpaque
        set(value) {(component as? JComponent)?.isOpaque = value}

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
        (component as? JComponent)?.border = when( border) {
            null -> null
            BASIC -> BorderFactory.createLineBorder( Skin.Global.BgDark.jcolor)
            BEVELED_LOWERED -> BorderFactory.createBevelBorder(BevelBorder.LOWERED, Skin.BevelBorder.Med.jcolor, Skin.BevelBorder.Dark.jcolor)
            BEVELED_RAISED -> BorderFactory.createBevelBorder(BevelBorder.RAISED, Skin.BevelBorder.Med.jcolor, Skin.BevelBorder.Dark.jcolor)
        }
    }

    override fun setColoredBorder(color: SColor, width: Int) {
        (component as? JComponent)?.border = BorderFactory.createLineBorder( color.jcolor, width)
    }

    // region ComponentListener
    private inner class ComponentMultiStack : ComponentListener {
        val resizeStack= EventStack<Unit>()
        val hiddenStack = EventStack<Unit>()
        val shownStack= EventStack<Unit>()
        val movedStack = EventStack<Unit>()

        init {
            component.addComponentListener(this)
        }

        override fun componentMoved(e: ComponentEvent?) {movedStack.triggers.forEach { it(Unit) }}
        override fun componentResized(e: ComponentEvent?) {resizeStack.triggers.forEach { it(Unit) }}
        override fun componentHidden(e: ComponentEvent?) {hiddenStack.triggers.forEach { it(Unit) }}
        override fun componentShown(e: ComponentEvent?) {shownStack.triggers.forEach { it(Unit) }}
    }

    private val componentMultiStack by lazy { ComponentMultiStack() }

    override val onResize get() = componentMultiStack.resizeStack
    override val onHidden get() = componentMultiStack.hiddenStack
    override val onShown get() = componentMultiStack.shownStack
    override val onMoved get() = componentMultiStack.movedStack
    // endregion

    // region MouseListener
    private inner class MouseMultiStack : java.awt.event.MouseListener, java.awt.event.MouseMotionListener
    {
        init {
            component.addMouseListener(this)
            component.addMouseMotionListener(this)
        }

        fun convert( e: JMouseEvent) : MouseEvent {
            val scomp = SwComponent(e.component as Component)
            val smask = e.modifiersEx
            val mask = MouseEvent.toMask(
                    (smask and SHIFT_DOWN_MASK) == SHIFT_DOWN_MASK,
                    (smask and CTRL_DOWN_MASK) == CTRL_DOWN_MASK,
                    (smask and ALT_DOWN_MASK) == ALT_DOWN_MASK)
            return MouseEvent(
                    SUIPoint(e.x, e.y, scomp.component),
                    when (e.button) {
                        JMouseEvent.BUTTON1 -> LEFT
                        JMouseEvent.BUTTON2 -> CENTER
                        JMouseEvent.BUTTON3 -> RIGHT
                        else -> UNKNOWN
                    },
                    mask)
        }

        val releaseStack = EventStack<MouseEvent>()
        val enterStack = EventStack<MouseEvent>()
        val clickStack = EventStack<MouseEvent>()
        val exitStack = EventStack<MouseEvent>()
        val pressStack = EventStack<MouseEvent>()
        val moveStack = EventStack<MouseEvent>()
        val dragStack = EventStack<MouseEvent>()

        override fun mouseReleased(e: JMouseEvent) {
            val evt = convert(e)
            releaseStack.triggers.forEach { it(evt) }
            clickStack.triggers.forEach { it(evt) }
        }
        override fun mouseEntered(e: JMouseEvent) {
            val evt = convert(e)
            enterStack.triggers.forEach { it(evt) }
        }
        override fun mouseClicked(e: JMouseEvent) {/*onMouseClick?.invoke(convert(e))*/}
        override fun mouseExited(e: JMouseEvent) {
            val evt = convert(e)
            exitStack.triggers.forEach { it(evt) }
        }
        override fun mousePressed(e: JMouseEvent) {
            val evt = convert(e)
            pressStack.triggers.forEach { it(evt) }
        }
        override fun mouseMoved(e: JMouseEvent) {
            val evt = convert(e)
            moveStack.triggers.forEach { it(evt) }
        }
        override fun mouseDragged(e: JMouseEvent) {
            val evt = convert(e)
            dragStack.triggers.forEach { it(evt) }
        }
    }

    private val mouseMultiStack get() = MouseMultiStack()

    override val onMouseClick get() = mouseMultiStack.clickStack
    override val onMousePress get() = mouseMultiStack.pressStack
    override val onMouseRelease get() = mouseMultiStack.releaseStack
    override val onMouseEnter get() = mouseMultiStack.enterStack
    override val onMouseExit get() = mouseMultiStack.exitStack
    override val onMouseMove get() = mouseMultiStack.moveStack
    override val onMouseDrag get() = mouseMultiStack.dragStack

    override fun markAsPassThrough() {
        component.addMouseMotionListener( object : MouseMotionListener {
            override fun mouseMoved(e: java.awt.event.MouseEvent?) = component.parent.dispatchEvent(e)
            override fun mouseDragged(e: java.awt.event.MouseEvent?) = component.parent.dispatchEvent(e)
        })
        component.addMouseListener( object : MouseListener {
            override fun mouseReleased(e: java.awt.event.MouseEvent?) = component.parent.dispatchEvent(e)
            override fun mouseEntered(e: java.awt.event.MouseEvent?) = component.parent.dispatchEvent(e)
            override fun mouseClicked(e: java.awt.event.MouseEvent?) = component.parent.dispatchEvent(e)
            override fun mouseExited(e: java.awt.event.MouseEvent?) = component.parent.dispatchEvent(e)
            override fun mousePressed(e: java.awt.event.MouseEvent?) = component.parent.dispatchEvent(e)
        })
    }

    // endregion

    override var onMouseWheelMoved: ((MouseWheelEvent) -> Unit)?
        get() = mouseWheelListener.onWheelMove
        set(value) {mouseWheelListener.onWheelMove = value}
    private val mouseWheelListener by lazy { JSMouseWheelListener().apply { component.addMouseWheelListener( this) }}
    private class JSMouseWheelListener( var onWheelMove : ((MouseWheelEvent)-> Unit)? = null) : MouseWheelListener {
        fun convert( e: java.awt.event.MouseWheelEvent) : MouseWheelEvent {
            val scomp = SwComponent(e.component as JComponent)
            return MouseWheelEvent(SUIPoint(e.x, e.y, scomp.component), e.wheelRotation)
        }

        override fun mouseWheelMoved(e: java.awt.event.MouseWheelEvent) {onWheelMove?.invoke(convert(e))}
    }

    override fun addEventOnKeypress(keycode: Int, modifiers: Int, action: () -> Unit) {
        (component as? JComponent)?.actionMap?.put(action, object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                action()
            }
        })
        (component as? JComponent)?.inputMap?.put(KeyStroke.getKeyStroke(keycode, modifiers), action)
    }

    override fun requestFocus() {
        component.requestFocus()
    }

}

class SwComponentIndirect(cGetter : Invokable<Component>) : ASwComponent() {
    override val component: Component by lazy { cGetter.invoker.invoke() }

}

class SwComponent(override val component: Component) : ASwComponent()
{
//    init {
//        SwCompMap.addMapping(component,this)
//        SwCompMap.ageOutMappings()
//    }
}


private object SwCompMap {
    val mapFromJCompHashCode = mutableMapOf<Int,MutableList<Pair<WeakReference<Component>,WeakReference<IComponent>>>>()

    fun addMapping( jComponent: Component, sComponent: IComponent) {
        val hash = jComponent.hashCode()
        val collision = mapFromJCompHashCode[jComponent.hashCode()]
        when( collision) {
            null -> mapFromJCompHashCode[hash] = mutableListOf(Pair(WeakReference(jComponent), WeakReference(sComponent)))
            else -> collision.add(Pair(WeakReference(jComponent), WeakReference(sComponent)))
        }
    }

    fun ageOutMappings() {
        mapFromJCompHashCode.entries
                .removeIf { entry ->
                    entry.value.removeIf { it.first.get() == null || it.second.get() == null}
                    entry.value.isEmpty()
                }
    }

    fun getMappingFrom( jComponent: Component) : IComponent?
    {
        val hash = jComponent.hashCode()
        return mapFromJCompHashCode[hash]?.firstOrNull{it.first.get() == jComponent}?.second?.get()
    }

}
