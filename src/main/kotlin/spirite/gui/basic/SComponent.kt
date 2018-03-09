package spirite.gui.basic

import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.JComponent
import javax.swing.JPanel

interface IComponent {
    var onResize : (() -> Unit)?
    var onHidden : (() -> Unit)?
    var onShown : (() -> Unit)?
    var onMoved : (() -> Unit)?
}

interface ISComponent : IComponent{
    val component : JComponent
}

class Invokable<T>() {
    lateinit var invoker : ()-> T
}

class SComponent( cGetter : Invokable<JComponent>) : ISComponent {
    override val component: JComponent by lazy { cGetter.invoker.invoke() }
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
            var onMoved : (() -> Unit)? = null) : ComponentListener
    {
        override fun componentMoved(e: ComponentEvent?) {onMoved?.invoke()}
        override fun componentResized(e: ComponentEvent?) {onResize?.invoke()}
        override fun componentHidden(e: ComponentEvent?) {onHidden?.invoke()}
        override fun componentShown(e: ComponentEvent?) {onShown?.invoke()}
    }
    private val componentListener by lazy {  JSComponentListener().apply { component.addComponentListener(this)}}
}

class JJJ( private val invokable: Invokable<JComponent> = Invokable()) : JPanel(), ISComponent by SComponent( invokable) {
    init {invokable.invoker = {this}}
}