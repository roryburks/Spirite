package spirite.gui.views.animation.structureView

import rbJvm.owl.addWeakObserver
import sgui.components.IComponent
import sgui.swing.SwIcon
import spirite.sguiHybrid.Hybrid
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.resources.SpiriteIcons

class AnimationStructureView(val master: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = SpiriteIcons.BigIcons.Frame_AnimationScheme
    override val name: String get() = "Animation Details"

    private val label = Hybrid.ui.Label(" ")
    private val subContainer = Hybrid.ui.CrossPanel()

    private val imp = Hybrid.ui.CrossPanel {
        rows.add(label)
        rows.addGap(2)
        rows += {
            addGap(2)
            add(subContainer, flex = 100f)
            addGap(2)
            flex = 100f
        }
        rows.addGap(2)
    }

    private var currentPanel : IComponent? = null

    private val _curAnimK = master.centralObservatory.currentAnimationBind.addWeakObserver { new, old ->
        when(new){
            old -> {}
            is FixedFrameAnimation -> {
                label.text = new.name
                subContainer.setLayout {
                    val ffapanel = AnimFFAStructPanel(master, new)
                    val scroll = Hybrid.ui.ScrollContainer(ffapanel)
                    ffapanel.scrollContext = scroll

                    rows.add(scroll, flex = 100f)
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
        _curAnimK.void()
    }
}