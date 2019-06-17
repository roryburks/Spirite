package spirite.gui.views.animation.animationSpaceView

import rb.glow.color.Colors
import rbJvm.owl.addWeakObserver
import rbJvm.owl.bindWeaklyTo
import sgui.generic.Orientation.VERTICAL
import sgui.generic.components.IComponent
import sgui.generic.components.IComponent.BasicBorder.BEVELED_RAISED
import sgui.generic.components.crossContainer.ICrossPanel
import sgui.swing.components.ResizeContainerPanel
import sgui.swing.skin.Skin
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.gui.resources.SpiriteIcons
import spirite.gui.views.animation.AnimationPlayView
import spirite.hybrid.Hybrid

class  FFAPlayView(
        val master: IMasterControl,
        val space: FFAAnimationSpace,
        private val imp : ICrossPanel = Hybrid.ui.CrossPanel())
    : IComponent by imp
{
    val drawView = AnimationPlayView()
    val btnPlayPause = Hybrid.ui.ToggleButton(false)
    val tfFPS = Hybrid.ui.FloatField()
    val bgColorBox = Hybrid.ui.ColorSquare(Skin.Global.Bg.scolor).also { it.setBasicBorder(BEVELED_RAISED) }

    val innerImpTop = Hybrid.ui.CrossPanel()
    val innerImpBottom = Hybrid.ui.CrossPanel()
    val resize = ResizeContainerPanel(innerImpTop, VERTICAL, 300)

    init {
        imp.setLayout {
            rows.add(resize, flex = 100f)
            rows.flex = 100f
        }

        btnPlayPause.setOnIcon(SpiriteIcons.BigIcons.Anim_Play)
        btnPlayPause.setOffIcon(SpiriteIcons.BigIcons.Anim_Play)

        resize.addPanel(innerImpBottom, 20,50,-1,false)

        innerImpBottom.setLayout {
            rows.add(Hybrid.ui.ScrollContainer(FFALexicalStagingView(master.dialog,space)))
        }

        innerImpBottom.background = Colors.BLACK

        innerImpTop.setLayout {
            rows.add(drawView, flex = 100f)
            rows+= {
                addGap(2)
                add(btnPlayPause, width=24,height = 24)
                addGap(2)
                add(Hybrid.ui.Label("FPS:"), height = 24)
                add(tfFPS, width = 200, height = 24)
                addGap(2)
                add(bgColorBox, width = 24, height = 24)
            }
        }
    }

    private val fpsK = tfFPS.valueBind.bindWeaklyTo(space.stateView.fpsBind)

    init /* Bindings */ {
        bgColorBox.colorBind.addWeakObserver { new, _ -> drawView.bgColor = new }

        Hybrid.timing.createTimer(50, true) {
            if( btnPlayPause.checked) {
                space.stateView.advance(50f)
                drawView.animation = space.stateView.animation
                drawView.frame = space.stateView.met
            }
        }
    }
}