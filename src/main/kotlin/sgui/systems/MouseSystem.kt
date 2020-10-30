package sgui.systems

import rb.global.IContract
import sgui.components.IComponent
import sgui.components.events.MouseEvent

/**
 * The SwMouseSystem gives components a semi-direct access to the Mouse Events broadcast by various components.  It gets
 * called regardless of what sub-panel is consuming it
 *
 * Priority vs non-priority hooks:
 *  The Mouse System will execute hooks in this order:
 *      -Priority Hooks, root-first
 *      -Non-priority Hooks, leaf-first
 */
interface IMouseSystem
{
    fun broadcastMouseEvent(mouseEvent: MouseEvent, root: Any)

    fun attachPriorityHook(hook: IGlobalMouseHook, component: IComponent) : IContract
    fun attachHook(hook: IGlobalMouseHook, component: IComponent) : IContract
}

interface IGlobalMouseHook
{
    fun processMouseEvent( evt: MouseEvent)
    fun overridesConsume(evt: MouseEvent) = false
}

