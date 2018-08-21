package spirite.gui.views.animation.animationSpaceView

import spirite.base.brains.IMasterControl
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.resources.IIcon
import spirite.hybrid.Hybrid

class AnimationSpaceView(private val master: IMasterControl) : IOmniComponent {
    override val component: IComponent
        get() = TODO("not implemented")
    override val icon: IIcon?
        get() = TODO("not implemented")
    override val name: String
        get() = TODO("not implemented")

    private val imp = Hybrid.ui.CrossPanel {

    }

    init {
        imp.ref = this
    }
}