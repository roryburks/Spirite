package spirite.gui.views.groupView

import spirite.gui.components.advanced.omniContainer.IOmniComponent
import sgui.generic.components.IComponent
import spirite.gui.resources.IIcon
import spirite.hybrid.Hybrid

class ReferenceView() : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = null
    override val name: String get() = "Ref"

    val imp = Hybrid.ui.Label("Reference View")

}