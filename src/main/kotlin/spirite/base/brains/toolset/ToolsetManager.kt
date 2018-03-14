package spirite.base.brains.toolset

import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.brains.toolset.IToolsetManager.ToolsetObserver

interface IToolsetManager {
    val toolset: Toolset
    var selectedTool: Tool

    interface ToolsetObserver {
        fun onToolChanged( newTool: Tool)
        fun onToolPropertyChanged( tool: Tool, property: ToolProperty<*>)
    }
    val toolsetObserver : IObservable<ToolsetObserver>
}

class ToolsetManager : IToolsetManager{
    override val toolsetObserver = Observable<ToolsetObserver>()
    override val toolset = Toolset(this)

    override var selectedTool: Tool = toolset.Pen
        set(value) {
            field = value
            toolsetObserver.trigger { it.onToolChanged(value) }
        }

    internal fun triggerToolsetChanged(tool: Tool, property: ToolProperty<*>) {
        toolsetObserver.trigger { it.onToolPropertyChanged(tool, property) }
    }
}