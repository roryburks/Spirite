package spirite.gui.basic

import spirite.gui.SUIPoint
import spirite.gui.basic.IComponent.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel

interface ISComponent : IComponent{
    val component : JComponent
}

abstract class ASComponent : ISComponent {
    override fun redraw() {component.repaint()}

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

        fun convert( e: java.awt.event.MouseEvent) : MouseEvent {
            val scomp = SComponentDirect( e.component as JComponent )
            return MouseEvent(SUIPoint(e.x, e.y, scomp))
        }

        override fun mouseReleased(e: java.awt.event.MouseEvent) { onMouseRelease?.invoke( convert(e))}
        override fun mouseEntered(e: java.awt.event.MouseEvent) { onMouseEnter?.invoke( convert(e))}
        override fun mouseClicked(e: java.awt.event.MouseEvent) {onMouseClick?.invoke(convert(e))}
        override fun mouseExited(e: java.awt.event.MouseEvent) {onMouseExit?.invoke(convert(e))}
        override fun mousePressed(e: java.awt.event.MouseEvent) {onMousePress?.invoke(convert(e))}
        override fun mouseMoved(e: java.awt.event.MouseEvent) {onMouseMove?.invoke(convert(e))}
        override fun mouseDragged(e: java.awt.event.MouseEvent) { onMouseDrag?.invoke(convert(e))}
    }

    // endregion
}

class SComponent( cGetter : Invokable<JComponent>) : ASComponent() {
    override val component: JComponent by lazy { cGetter.invoker.invoke() }

}

class SComponentDirect( override val component: JComponent) : ASComponent()

