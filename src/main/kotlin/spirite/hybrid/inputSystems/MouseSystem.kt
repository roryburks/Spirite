package spirite.hybrid.inputSystems

import rb.owl.IContract
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseEvent.MouseEventType.*

/**
 * The MouseSystem gives components a semi-direct access to the Mouse Events broadcast by various components.  It gets
 * called regardless of what sub-panel is consuming it
 */
interface IMouseSystem
{
    fun broadcastMouseEvent( mouseEvent: MouseEvent, root: Any)

    fun attachHook(hook: IGlobalMouseHook, component: IComponent, root: Any) : IContract
}

interface IGlobalMouseHook
{
    fun processMouseEvent( evt: MouseEvent)
}

class MouseSystem : IMouseSystem
{
    private inner class Contract
    constructor(
            val hook: IGlobalMouseHook,
            val component: IComponent,
            val root : Any)
        : IContract
    {
        init {_hooks.add(this)}
        override fun void() {_hooks.remove(this)}
    }

    private val _hooks = mutableListOf<Contract>()
    private var _grabbedComponents : Set<Contract>? = null

    override fun broadcastMouseEvent(mouseEvent: MouseEvent, root: Any) {
        val overlappingComponents = _hooks.asSequence()
                .filter { it.root == root }
                .filter { mouseEvent.point.convert(it.component)
                        .run { x >= 0 && x <= it.component.width && y >= 0 && y <= it.component.height }}
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

    override fun attachHook(hook: IGlobalMouseHook, component: IComponent, root: Any) : IContract = Contract(hook, component, root)

}