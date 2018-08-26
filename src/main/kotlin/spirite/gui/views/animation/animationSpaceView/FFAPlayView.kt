package spirite.gui.views.animation.animationSpaceView

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.gui.Orientation.VERTICAL
import spirite.gui.components.advanced.ResizeContainerPanel
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid

class  FFAPlayView(
        val master: IMasterControl,
        val space: FFAAnimationSpace,
        private val imp : ICrossPanel= Hybrid.ui.CrossPanel())
    : IComponent by imp
{
    val drawView = Hybrid.ui.CrossPanel()
    val btnPlayPause = Hybrid.ui.ToggleButton(false)
    val tfFPS = Hybrid.ui.FloatField()

    val innerImpTop = Hybrid.ui.CrossPanel()
    val innerImpBottom = Hybrid.ui.CrossPanel()
    val resize = ResizeContainerPanel(innerImpTop,VERTICAL, 300)

    init {
        imp.setLayout {
            rows.add(resize, flex = 100f)
            rows.flex = 100f
        }

        resize.addPanel(innerImpBottom, 20,50,-1,false)

        innerImpBottom.setLayout {
            rows.add(Hybrid.ui.ScrollContainer(FFALexicalStagingView(master.dialog,space)))
        }

        innerImpTop.setLayout {
            rows.add(drawView, flex = 100f)
            rows+= {
                addGap(2)
                add(btnPlayPause, width=24,height = 24)
                addGap(2)
                add(Hybrid.ui.Label("FPS:"), height = 24)
                add(tfFPS, width = 200, height = 24)
            }
        }
    }

    init {
        space.stateView.fpsBind.bindWeakly(tfFPS.valueBind)
        btnPlayPause.setOnIcon(SwIcons.BigIcons.Anim_Play)
        btnPlayPause.setOffIcon(SwIcons.BigIcons.Anim_Play)

        Hybrid.timing.createTimer(50, true) {
            if( btnPlayPause.checked) {
                space.stateView.advance(50f)
            }
        }
    }

}