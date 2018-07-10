package spirite.gui.components.major.animation

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.major.animation.structureView.AnimFFAStructPanel
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid

class AnimationStructureView(val master: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = SwIcons.BigIcons.Frame_AnimationScheme
    override val name: String get() = "Animation Details"

    private val label = Hybrid.ui.Label(" ")
    private val subContainer = Hybrid.ui.CrossPanel()
    private val scrollContainer = Hybrid.ui.ScrollContainer(subContainer)

    private val imp = Hybrid.ui.CrossPanel {
        rows.add(label)
        rows.addGap(2)
        rows += {
            addGap(2)
            add(scrollContainer, flex = 100f)
            addGap(2)
            flex = 100f
        }
        rows.addGap(2)
    }

    private val _animationBind = master.centralObservatory.currentAnimationBind.addListener { new, old ->
        when(new){
            old -> {}
            is FixedFrameAnimation -> {
                label.text = new.name
                subContainer.setLayout {
                    rows.add(AnimFFAStructPanel(new), flex = 100f)
                    rows.flex = 100f
                }
            }
            else -> {
                label.text = " "
                subContainer.clearLayout()
            }
        }

    }

    override fun close() {
        _animationBind.unbind()
    }
}