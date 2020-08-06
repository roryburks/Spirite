package spirite.gui.views.groupView

import sgui.components.IComponent
import sguiSwing.SwIcon
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import sguiSwing.hybrid.Hybrid

class ReferenceView() : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = null
    override val name: String get() = "Ref"

    val imp = Hybrid.ui.Label("Reference View")

}