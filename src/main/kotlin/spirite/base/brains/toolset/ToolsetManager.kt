package spirite.base.brains.toolset

import spirite.base.brains.Bindable
import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.brains.toolset.IToolsetManager.ToolsetPropertyObserver

interface IToolsetManager {
    val toolset: Toolset

    val selectedToolBinding: Bindable<Tool>
    var selectedTool: Tool

    interface ToolsetPropertyObserver {
        fun onToolPropertyChanged( tool: Tool, property: ToolProperty<*>)
    }
    val toolsetObserver : IObservable<ToolsetPropertyObserver>
}

class ToolsetManager : IToolsetManager{
    override val toolsetObserver = Observable<ToolsetPropertyObserver>()
    override val toolset = Toolset(this)

    override val selectedToolBinding = Bindable<Tool>(toolset.Pen)
    override var selectedTool by selectedToolBinding

    internal fun triggerToolsetChanged(tool: Tool, property: ToolProperty<*>) {
        toolsetObserver.trigger { it.onToolPropertyChanged(tool, property) }
    }
}