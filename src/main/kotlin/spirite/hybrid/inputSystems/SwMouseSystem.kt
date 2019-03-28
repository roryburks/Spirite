package spirite.hybrid.inputSystems

import rb.extendo.extensions.append
import rb.extendo.extensions.deref
import rb.extendo.extensions.lookup
import rb.owl.IContract
import spirite.gui.SUIPoint
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseEvent.MouseEventType.*
import spirite.pc.gui.basic.SwComponent
import java.awt.Component
import java.lang.ref.WeakReference
import javax.swing.SwingUtilities

/**
 * The SwMouseSystem gives components a semi-direct access to the Mouse Events broadcast by various components.  It gets
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
    fun overridesConsume(evt: MouseEvent) = false
}

class SwMouseSystem : IMouseSystem
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
            component: IComponent)
        : IContract
    {
        val hc = component.component.hashCode()
        val compRef = WeakReference(component)
        var root : Any? = null
        init {_hooks.append(hc, this)}
        override fun void() {_hooks.deref(hc, this)}
    }

    private val _hooks = mutableMapOf<Int,MutableList<Contract>>()
    private var _grabbedComponents : Set<Contract>? = null

    override fun broadcastMouseEvent(mouseEvent: MouseEvent, root: Any) {
        _hooks.forEach { _, u -> u.removeIf { it.compRef.get() == null } }

        mouseEvent.point as SUIPoint
        root as Component
        val systemCoordinates =SwingUtilities.convertPoint(
                mouseEvent.point.component,
                mouseEvent.point.x,
                mouseEvent.point.y,
                root)

        val deepestComponent = SwingUtilities.getDeepestComponentAt(root, systemCoordinates.x, systemCoordinates.y)

        val ancestry = mutableListOf<Component>()
        var curComp: Component? = deepestComponent
        while (curComp != null) {
            ancestry.add(0, curComp)
            curComp = curComp.parent
        }

        if( mouseEvent.type == DRAGGED || mouseEvent.type == RELEASED) {
            _grabbedComponents?.forEach {
                val comp = it.compRef.get() ?: return@forEach
                val localMouseEvent by lazy { mouseEvent.converted( comp) }
                it.hook.processMouseEvent(localMouseEvent)
            }

            if( mouseEvent.type == RELEASED)
                _grabbedComponents = null
        }
        else {
            val triggeredContracts = mutableListOf<Contract>()

            var consumed = false
            ancestry.forEach {
                val triggers = _hooks.lookup(it.hashCode())

                val localMouseEvent by lazy { mouseEvent.converted(SwComponent(it)) }

                val triggerContractsForThis = triggers.filter { !consumed || it.hook.overridesConsume(localMouseEvent) }
                triggeredContracts.addAll(triggerContractsForThis)
                triggerContractsForThis.forEach { it.hook.processMouseEvent(localMouseEvent) }

                consumed = consumed || localMouseEvent.consumed
            }

            val triggeredContractsAsHashSet = triggeredContracts.toHashSet()
            if (mouseEvent.type == DRAGGED || mouseEvent.type == RELEASED) {
            }

            if (mouseEvent.type == PRESSED)
                _grabbedComponents = triggeredContractsAsHashSet
        }
    }

    override fun attachHook(hook: IGlobalMouseHook, component: IComponent) : IContract {
        val contract = Contract(hook, component)

        SwingUtilities.invokeLater {
            contract.root = SwingUtilities.getRoot(component.component  as Component)
        }

        return contract
    }

}