package spirite.gui.components.major.tool

import spirite.base.brains.IMasterControl
import spirite.base.brains.toolset.Tool
import spirite.gui.components.basic.IBoxList
import spirite.gui.components.basic.IComponent
import spirite.hybrid.Hybrid

class ToolSection (
        private val master: IMasterControl,
        val imp : IBoxList<Tool> = Hybrid.ui.BoxList(24, 24, null))
    :IComponent by imp
{
    init {
        master.centralObservatory.activeDrawerBind.addListener { new, old ->
            imp.addEntry(master.toolsetManager.toolset.Pen)
        }
    }

}
