package spirite.gui.views.groupView

import spirite.gui.components.advanced.omniContainer.IOmniComponent
import sgui.generic.components.IComponent
import sgui.swing.SwIcon
import spirite.hybrid.Hybrid

class ReferenceView() : IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = null
    override val name: String get() = "Ref"

    val imp = Hybrid.ui.Label("Reference View")

}