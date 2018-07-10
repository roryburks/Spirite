package spirite.gui.components.major.animation.structureView

import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationObserver
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BASIC
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.ILabel
import spirite.gui.resources.Skin
import spirite.hybrid.Hybrid
import spirite.pc.gui.JColor
import spirite.pc.gui.basic.SwComponent
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.max

class AnimFFAStructPanel(
        val anim: FixedFrameAnimation,
        private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
    : IComponent by imp
{
    private var nameWidth = 60
    private var layerHeight = 24

    private var tickWidth = 24
    private var tickHeight = 16

    fun rebuild() {
        imp.setLayout {
            val start = anim.start
            val end = anim.end

            anim.layers.forEach {layer ->
                rows += {
                    add(NamePanel(layer), width = nameWidth )

                    layer.frames.forEach {
                        when( it.marker) {
                            FRAME -> {add(GapPanel(), width = tickWidth)}
                            START_LOCAL_LOOP -> {}
                            END_LOCAL_LOOP -> {}
                        }
                    }

                    height = layerHeight
                }
            }

            rows += {
                addGap(nameWidth)
                (start until end).forEach { add(TickPanel(it), width = tickWidth) }
                height = tickHeight
            }
        }
    }

    private inner class NamePanel(val layer: FFALayer, private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        init {
            imp.setBasicBorder(BEVELED_LOWERED)
            imp.background = Skin.Global.BgDark.scolor

            imp.setLayout {
                rows.add(Hybrid.ui.Label("layer"))
            }

            imp.onMouseRelease = { print("click")}
        }
    }

    private inner class TickPanel( val tick: Int, private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        :ICrossPanel by imp
    {
        init {
            imp.setLayout {
                rows.add(Hybrid.ui.Label("$tick"))
            }
        }
    }

    private inner class GapPanel() : IComponent by SwComponent(DashedOutPanel(Skin.Global.BgDark.jcolor, Skin.Global.Fg.jcolor))


    // region Listener/Observer Bindings
    private val _x = object : AnimationStructureChangeObserver {
        override fun animationStructureChanged(animation: Animation) {
            if( animation == anim)
                rebuild()
        }
    }.also { anim.workspace.animationManager.animationStructureChangeObservable.addObserver( it)}
    // endregion

    init {
        imp.background = Skin.Global.BgDark.scolor
        rebuild()
    }
}

private class DashedOutPanel(val bgcol: JColor, val fgcol: JColor) : JPanel() {
    override fun paintComponent(g: Graphics) {
        g.color = bgcol
        g.fillRect(0,0,width, height)

        g.color = fgcol
        (0.. (width + height)/4)
                .forEach { g.drawLine(0, it*4, it*4, 0)}

    }

}