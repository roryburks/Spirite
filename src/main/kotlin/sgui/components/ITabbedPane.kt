package sgui.components

import rb.owl.bindable.Bindable

interface ITabbedPane : IComponent
{
    val selectedIndexBind : Bindable<Int>
    var selectedIndex: Int

    val tabCount : Int
    val components: List<IComponent?>
    val titles: List<String>

    fun addTab( title : String, component: IComponent?)
    fun setComponentAt( index: Int, newComponent: IComponent?)
    fun setTitleAt(index: Int, newTitle: String)
    fun removeTabAt( index: Int)
}

abstract class TabbedPanePartial : ITabbedPane {
    data class Tab(
            var title: String,
            var component: IComponent?)
    protected val tabs = mutableListOf<Tab>()

    override val selectedIndexBind = Bindable(-1)
    override var selectedIndex: Int
        get() = selectedIndexBind.field
        set(value) {selectedIndexBind.field = if( value < -1  || value >= tabs.size) -1 else value}

    override val tabCount: Int get() = tabs.size

    override val components: List<IComponent?> get() = tabs.map { it.component }
    override val titles: List<String> get() = tabs.map { it.title }
}

