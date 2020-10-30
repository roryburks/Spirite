package sguiSwing.components

import rbJvm.glow.SColor
import sgui.UIPoint
import sgui.components.IComponent
import sgui.components.IComponent.*
import sgui.components.IComponent.BasicBorder.*
import sgui.components.Invokable
import sgui.components.events.MouseEvent
import sgui.components.events.MouseEvent.MouseEventType.*
import sgui.components.events.MouseWheelEvent
import sgui.systems.IGlobalMouseHook
import sguiSwing.jcolor
import sguiSwing.mouseSystem.SwMouseSystem
import sguiSwing.scolor
import sguiSwing.skin.Skin
import java.awt.Component
import java.awt.Cursor
import java.awt.event.*
import java.lang.ref.WeakReference
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.border.BevelBorder

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

    override val topLeft: UIPoint get() = sguiSwing.SUIPoint(x, y, this.component.parent)
    override val bottomRight: UIPoint get() = sguiSwing.SUIPoint(x + width, y + height, this.component.parent)

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
    private inner class MouseMultiStack : IGlobalMouseHook
    {
        init {
            SwMouseSystem.attachHook(this, this@ASwComponent)
        }

        override fun processMouseEvent(evt: MouseEvent) {
            when(evt.type) {
                PRESSED ->  pressStack.triggers.forEach { it(evt) }
                RELEASED -> {
                    releaseStack.triggers.forEach { it(evt) }
                    clickStack.triggers.forEach {it(evt.copy(type = CLICKED))}
                }
                CLICKED -> {}
                ENTERED -> enterStack.triggers.forEach { it(evt) }
                EXITED -> exitStack.triggers.forEach { it(evt) }
                MOVED -> moveStack.triggers.forEach { it(evt) }
                DRAGGED -> dragStack.triggers.forEach { it(evt) }
            }
        }

        val releaseStack = EventStack<MouseEvent>()
        val enterStack = EventStack<MouseEvent>()
        val clickStack = EventStack<MouseEvent>()
        val exitStack = EventStack<MouseEvent>()
        val pressStack = EventStack<MouseEvent>()
        val moveStack = EventStack<MouseEvent>()
        val dragStack = EventStack<MouseEvent>()
    }

    private val mouseMultiStack by lazy { MouseMultiStack() }

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
            return MouseWheelEvent(sguiSwing.SUIPoint(e.x, e.y, scomp.component), e.wheelRotation)
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
                .removeAll { entry ->
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
