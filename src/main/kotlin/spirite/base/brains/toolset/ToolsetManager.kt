package spirite.base.brains.toolset

import spirite.base.util.binding.CruddyBindable
import spirite.base.brains.ICruddyOldObservable
import spirite.base.brains.CruddyOldObservable
import spirite.base.brains.toolset.IToolsetManager.ToolsetPropertyObserver

interface IToolsetManager {
    val toolset: Toolset

    val selectedToolBinding: CruddyBindable<Tool>
    var selectedTool: Tool

    interface ToolsetPropertyObserver {
        fun onToolPropertyChanged( tool: Tool, property: ToolProperty<*>)
    }
    val toolsetObserver : ICruddyOldObservable<ToolsetPropertyObserver>
}

class ToolsetManager : IToolsetManager{
    override val toolsetObserver = CruddyOldObservable<ToolsetPropertyObserver>()
    override val toolset = Toolset(this)

    override val selectedToolBinding = CruddyBindable<Tool>(toolset.Pen)
    override var selectedTool by selectedToolBinding

    internal fun triggerToolsetChanged(tool: Tool, property: ToolProperty<*>) {
        toolsetObserver.trigger { it.onToolPropertyChanged(tool, property) }
    }
}