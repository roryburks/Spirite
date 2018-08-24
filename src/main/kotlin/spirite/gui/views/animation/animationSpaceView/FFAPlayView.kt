package spirite.gui.views.animation.animationSpaceView

import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid

class  FFAPlayView(
        val space: FFAAnimationSpace,
        private val imp : ICrossPanel= Hybrid.ui.CrossPanel())
    : IComponent by imp
{
    val drawView = Hybrid.ui.CrossPanel()
    val btnPlayPause = Hybrid.ui.ToggleButton(false)
    val tfFPS = Hybrid.ui.FloatField()

    init {
        imp.setLayout {
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

        //1000 / 50 = ticks per second
        // fps = frames per second
        // frames per tick = (frames per second) / (ticks per second)

        Hybrid.timing.createTimer(50, true) {
            if( btnPlayPause.checked) {
                space.stateView.advance(50f)
            }
        }
    }

}