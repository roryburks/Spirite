package spirite.hybrid.inputSystems

import rb.owl.IContract
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseEvent.MouseEventType.*
import java.awt.Component
import java.awt.Rectangle
import java.lang.ref.WeakReference
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * The MouseSystem gives components a semi-direct access to the Mouse Events broadcast by various components.  It gets
 * called regardless of what sub-panel is consuming it
 */
interface IMouseSystem
{
    fun broadcastMouseEvent( mouseEvent: MouseEvent, root: Any)

    fun attachHook(hook: IGlobalMouseHook, component: IComponent) : IContract
}

interface IGlobalMouseHook
{
    fun processMouseEvent( evt: MouseEvent)
}

class MouseSystem : IMouseSystem
{
    /***
     * Note, the Mouse System has a weak reference to the Intermediate UI component with the expectation that
     * the Intermediate UI component is integrated into the actual UI components life.
     *
     * If the drops out
     */

    private inner class Contract
    constructor(
            val hook: IGlobalMouseHook,
            val component: WeakReference<IComponent>)
        : IContract
    {
        var root : Any? = null
        init {_hooks.add(this)}
        override fun void() {_hooks.remove(this)}
    }

    private val _hooks = mutableListOf<Contract>()
    private var _grabbedComponents : Set<Contract>? = null

    override fun broadcastMouseEvent(mouseEvent: MouseEvent, root: Any) {
        _hooks.removeIf { it.component.get() == null }

        val overlappingComponents = _hooks.asSequence()
                .filter { it.root == root }
                .filter {
                    val component = it.component.get() ?: return@filter false
                    val point = mouseEvent.point.convert(component)
                    val visibleRect = (component.component as? JComponent)?.visibleRect ?:
                            Rectangle(0,0, component.width, component.height)
                    visibleRect.contains(point.x, point.y)
                }
                .toSet()


        val triggeringHooks = when(mouseEvent.type) {
            DRAGGED, RELEASED -> overlappingComponents.union(_grabbedComponents ?: emptySet())
            else -> overlappingComponents
        }

        for (hook in triggeringHooks) {

            hook.hook.processMouseEvent(mouseEvent)
        }

        if(mouseEvent.type == PRESSED)
            _grabbedComponents = overlappingComponents
        if( mouseEvent.type == RELEASED)
            _grabbedComponents = null
    }

    override fun attachHook(hook: IGlobalMouseHook, component: IComponent) : IContract {
        val contract = Contract(hook, WeakReference(component))

        SwingUtilities.invokeLater {
            contract.root = SwingUtilities.getRoot(component.component  as Component)
        }

        return contract
    }

}