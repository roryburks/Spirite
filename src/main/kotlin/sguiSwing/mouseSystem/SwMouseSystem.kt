package sguiSwing.mouseSystem

import rb.extendo.extensions.append
import rb.extendo.extensions.deref
import rb.extendo.extensions.lookup
import rb.global.IContract
import sgui.components.IComponent
import sgui.components.events.MouseEvent
import sgui.components.events.MouseEvent.MouseEventType.*
import sgui.systems.IGlobalMouseHook
import sgui.systems.IMouseSystem
import sguiSwing.components.SwComponent
import java.awt.Component
import java.lang.ref.WeakReference
import javax.swing.SwingUtilities

val SwMouseSystem : IMouseSystem = SwMouseSystem_imp()

private class SwMouseSystem_imp : IMouseSystem
{
    /***
     * Note, the Mouse System has a weak reference to the Intermediate UI component with the expectation that
     * the Intermediate UI component is integrated into the actual UI components life.
     *
     * If the drops out
     */

    private abstract inner  class AbstractContract
    constructor(
            val hook: IGlobalMouseHook,
            component: IComponent)
        : IContract
    {
        val hc = component.component.hashCode()
        val compRef = WeakReference(component)
        var root : Any? = null
    }

    private inner class Contract
    constructor(hook: IGlobalMouseHook, component: IComponent)
        : AbstractContract(hook, component)
    {
        init {_hooks.append(hc, this)}
        override fun void() {_hooks.deref(hc, this)}
    }

    private inner class PriorityContract
    constructor(hook: IGlobalMouseHook, component: IComponent)
        : AbstractContract(hook, component)
    {
        init {_priorityHooks.append(hc, this)}
        override fun void() {_priorityHooks.deref(hc, this)}
    }

    private val _hooks = mutableMapOf<Int,MutableList<Contract>>()
    private val _priorityHooks = mutableMapOf<Int,MutableList<PriorityContract>>()
    private var _grabbedComponents : Set<AbstractContract>? = null

    override fun broadcastMouseEvent(mouseEvent: MouseEvent, root: Any) {
        _hooks.forEach { _, u -> u.removeIf { it.compRef.get() == null } }

        mouseEvent.point as sguiSwing.SUIPoint
        root as Component
        val systemCoordinates = SwingUtilities.convertPoint(
                mouseEvent.point.component,
                mouseEvent.point.x,
                mouseEvent.point.y,
                root)

        val deepestComponent = SwingUtilities.getDeepestComponentAt(root, systemCoordinates.x, systemCoordinates.y)

        val rootFirstAncestry = mutableListOf<Component>()
        val leafFirstAncestry = mutableListOf<Component>()
        var curComp: Component? = deepestComponent
        while (curComp != null) {
            rootFirstAncestry.add(0, curComp)
            leafFirstAncestry.add(curComp)
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
            val triggeredContracts = mutableListOf<AbstractContract>()

            var consumed = false
            rootFirstAncestry.forEach {
                val triggers = _priorityHooks.lookup(it.hashCode())

                val localMouseEvent by lazy { mouseEvent.converted(SwComponent(it)) }

                val triggerContractsForThis  = triggers.filter { !consumed || it.hook.overridesConsume(localMouseEvent) }
                triggeredContracts.addAll(triggerContractsForThis)
                triggerContractsForThis.forEach { it.hook.processMouseEvent(localMouseEvent) }

                consumed = consumed || localMouseEvent.consumed
            }
            leafFirstAncestry.forEach {
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
            contract.root = SwingUtilities.getRoot(component.component as Component)
        }

        return contract
    }

    override fun attachPriorityHook(hook: IGlobalMouseHook, component: IComponent): IContract {
        val contract = PriorityContract(hook, component)

        SwingUtilities.invokeLater {
            contract.root = SwingUtilities.getRoot(component.component as Component)
        }

        return contract
    }

}