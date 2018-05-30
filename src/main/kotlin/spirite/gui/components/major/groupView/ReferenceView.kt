package spirite.gui.components.major.groupView

import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.resources.IIcon
import spirite.hybrid.Hybrid

class ReferenceView() : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = null

    val imp = Hybrid.ui.Label("Reference View")

}