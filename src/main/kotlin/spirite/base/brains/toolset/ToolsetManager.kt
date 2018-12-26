package spirite.base.brains.toolset

import rb.owl.IObservable
import rb.owl.Observable
import rb.owl.bindable.Bindable
import spirite.base.brains.toolset.IToolsetManager.ToolsetPropertyObserver

interface IToolsetManager {
    val toolset: Toolset

    val selectedToolBinding: Bindable<Tool>
    var selectedTool: Tool

    interface ToolsetPropertyObserver {
        fun onToolPropertyChanged( tool: Tool, property: ToolProperty<*>)
    }
    val toolsetObservable : IObservable<ToolsetPropertyObserver>
}

class ToolsetManager : IToolsetManager{
    override val toolsetObservable = Observable<ToolsetPropertyObserver>()
    override val toolset = Toolset(this)

    override val selectedToolBinding = Bindable<Tool>(toolset.Pen)
    override var selectedTool by selectedToolBinding

    internal fun triggerToolsetChanged(tool: Tool, property: ToolProperty<*>) {
        toolsetObservable.trigger { it.onToolPropertyChanged(tool, property) }
    }
}