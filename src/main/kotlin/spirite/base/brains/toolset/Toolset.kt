package spirite.base.brains.toolset

import spirite.base.imageData.mediums.drawer.IImageDrawer

/** A Toolset is a complete set of every tool */
class Toolset( internal val manager: ToolsetManager) {
    val Pen = Pen(this)


    private val defaultTools get() = listOf(Pen)
    fun toolsForDrawer(drawer: IImageDrawer) : List<Tool> {
        return defaultTools
    }
}