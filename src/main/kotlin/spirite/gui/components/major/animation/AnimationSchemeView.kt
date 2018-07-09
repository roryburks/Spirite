package spirite.gui.components.major.animation

import spirite.base.brains.IMasterControl
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid

class AnimationSchemeView(val master: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = SwIcons.BigIcons.Frame_AnimationScheme
    override val name: String get() = "Anims"

    val imp = Hybrid.ui.Label("Animations")
}