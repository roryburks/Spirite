package spirite.gui.crossLayout

import spirite.gui.Basic.SComponent

class CrossLayoutGroup(
        val vertical : Boolean)
    : CrossLayoutGroupEntry()
{
    val entries : List<CrossLayoutGroupEntry> get() = _entries
    private val _entries = mutableListOf<CrossLayoutGroupEntry>()

    fun addGap( size: Int) {_entries += CLG_Gap(size)}
    fun addGap( minSize: Int, defaultSize: Int, maxSize: Int) {_entries += CLG_Gap(minSize, defaultSize, maxSize)}

    fun addGroup( initializer: (CrossLayoutGroup) -> Unit) {
        val group = CrossLayoutGroup(!vertical)
        initializer.invoke(group)
    }

    fun addComponent(component: SComponent, size: Int? = null) {

    }
}

sealed class CrossLayoutGroupEntry {

}
internal class CLG_Gap(
        val minSize: Int,
        val defaultSize: Int,
        val maxSize: Int) : CrossLayoutGroupEntry()
{
    constructor(size : Int) : this( size, size, size)
}