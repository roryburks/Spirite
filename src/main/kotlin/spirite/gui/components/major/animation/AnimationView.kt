package spirite.gui.components.major.animation

import spirite.base.brains.IMasterControl
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.basic.SwPanel
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel

class AnimationView(val masterControl: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = SwIcons.BigIcons.Frame_AnimationScheme

    private val viewPanel = AnimationViewPanel()
    private val btnPrev = Hybrid.ui.Button().also { it.setIcon(SwIcons.BigIcons.Anim_StepB) }
    private val btnPlay = Hybrid.ui.Button().also { it.setIcon(SwIcons.BigIcons.Anim_Play) }
    private val btnNext = Hybrid.ui.Button().also { it.setIcon(SwIcons.BigIcons.Anim_StepF) }
    private val ffFps = Hybrid.ui.FloatField()

    val imp = Hybrid.ui.CrossPanel {
        rows += {
            add( viewPanel, flex = 300f)
            flex = 300f
        }
        rows.addGap(3)
        rows += {
            add(btnPrev, width = 24, height = 24)
            addGap(2)
            add(btnPlay, width = 24, height = 24)
            addGap(2)
            add(btnNext, width = 24, height = 24)
            addGap(5)
            add(ffFps, width = 128)
            add(Hybrid.ui.Label("FPS"))
            addGap(3)

        }
        rows.padding = 3

    }

    init {
        viewPanel.imp.context = this
    }
}

private class AnimationViewPanel(val imp : AnimationViewPanelImp = AnimationViewPanelImp()) : IComponent by SwComponent(imp) {

    class AnimationViewPanelImp : JPanel() {
        var context : AnimationView? = null

        override fun paintComponent(g: Graphics) {
            val context = context
            if( context == null)
                super.paintComponent(g)
            else
            {
                g.color = Color.RED
                g.fillRect(10, 10, width - 20, height - 20)
            }
        }
    }
}