package spirite.gui.components.major.animation.structureView

import spirite.base.brains.IMasterControl
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.IImageObservatory.ImageObserver
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.util.round
import spirite.gui.Direction
import spirite.gui.Direction.*
import spirite.gui.components.advanced.crossContainer.CrossInitializer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.events.MouseEvent.MouseButton
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.gui.resources.Skin
import spirite.hybrid.Hybrid
import spirite.pc.gui.JColor
import spirite.pc.gui.basic.SwComponent
import java.awt.Graphics
import java.awt.event.KeyEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class AnimFFAStructPanel
private constructor(
        val master: IMasterControl,
        val anim: FixedFrameAnimation,
        private val imp: Imp)
    : IComponent by SwComponent(imp)
{
    constructor(master: IMasterControl, anim: FixedFrameAnimation) : this(master, anim, Imp())
    init {imp.context = this}

    private var nameWidth = 60
    private var layerHeight = 24

    private var tickWidth = 24
    private var tickHeight = 16

    private val frameLinks = mutableListOf<FramePanel>()

    fun rebuild() {
        frameLinks.clear()

        imp.setLayout {
            val start = anim.start
            val end = anim.end

            anim.layers.forEach {layer ->
                rows += {
                    add(NamePanel(layer), width = nameWidth )

                    var len = 0
                    layer.frames.forEach {
                        len += it.length
                        when( it.marker) {
                            FRAME -> {
                                if (it.gapBefore > 0)
                                    add(GapPanel(it, true), width = tickWidth * it.gapBefore)
                                if (it.innerLength > 0)
                                    add( FramePanel(it).also {frameLinks.add(it) }, width = tickWidth * it.innerLength)
                                if (it.gapAfter > 0)
                                    add(GapPanel(it, false), width = tickWidth * it.gapAfter)
                            }
                            START_LOCAL_LOOP -> {}
                            END_LOCAL_LOOP -> {}
                        }
                    }

                    if( len < end) {
                        add(BlankPanel(), width = (end - len) * tickWidth)
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
            val label = Hybrid.ui.EditableLabel("layer")
            imp.setLayout { rows.add(label) }
            imp.onMouseRelease = {label.requestFocus()}
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

    private inner class GapPanel(val frame: FFAFrame, before: Boolean) : IComponent by SwComponent(DashedOutPanel(Skin.Global.BgDark.jcolor, Skin.Global.Fg.jcolor))
    {
        init {
        }
    }

    private inner class BlankPanel() : IComponent by SwComponent(DashedOutPanel(Skin.Global.BgDark.jcolor, Skin.Global.Bg.jcolor))


    private inner class FramePanel(
            val frame: FFAFrame,private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        val imageBox = Hybrid.ui.ImageBox(master.thumbBi.accessThumbnail(frame.node!!, anim.workspace))
        init {
            imp.setLayout {
                cols.add(imageBox , width = tickWidth)

                if( frame.innerLength > 1) {
                    cols.add(SwComponent(ArrowPanel(Skin.Global.BgDark.jcolor, Skin.FFAAnimation.Arrow.jcolor, RIGHT)), width = tickWidth * (frame.innerLength-1))
                }

                cols.height = layerHeight
            }

            imp.onMouseRelease = {evt ->
                if( evt.button == MouseButton.RIGHT) {
                    master.contextMenus.LaunchContextMenu(
                            bottomRight,
                            listOf(
                                    MenuItem("Add Gap &Before", customAction = {frame.gapBefore+=1}),
                                    MenuItem("Add Gap &After", customAction = {frame.gapAfter+=1}),
                                    MenuItem("Increase &Length", customAction = {frame.innerLength+=1}) )
                    )
                }
            }
        }

        fun refreshThumbnail() {
            imageBox.setImage( master.thumbBi.accessThumbnail(frame.node!!, anim.workspace))
        }
    }

    private class Imp : JPanel() {
        lateinit var context : AnimFFAStructPanel

        fun setLayout( constructor: CrossInitializer.()->Unit) {
            removeAll()
            val list = mutableListOf<IComponent>()
            layout = CrossLayout.buildCrossLayout(this, list, constructor)
            validate()
            CrossLayout.buildCrossLayout(this, null, constructor)
        }
    }

    // region Listener/Observer Bindings
    private val _animationStructureObserver = object : AnimationStructureChangeObserver {
        override fun animationStructureChanged(animation: Animation) {
            if( animation == anim)
                rebuild()
        }
    }.also {anim.workspace.animationManager.animationStructureChangeObservable.addObserver( it)}

    private val _imageObserver = object : ImageObserver {
        override fun imageChanged(evt: ImageChangeEvent) {
            frameLinks.forEach { it.refreshThumbnail() }
        }
    }.also { anim.workspace.imageObservatory.imageObservable.addObserver(it) }
    // endregion

    init {
        imp.background = Skin.Global.BgDark.jcolor
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
private class ArrowPanel(val bgcol: JColor, val fgcol: JColor, val dir: Direction) : JPanel() {
    override fun paintComponent(g: Graphics) {

        g.color = bgcol
        g.fillRect(0,0,width, height)

        g.color = fgcol

        val w = width
        val h = height

        val logical_x = listOf( 0.05, 0.7, 0.7, 0.95, 0.7, 0.7, 0.05)
        val logical_y = listOf( 0.35, 0.35, 0.15, 0.5, 0.85, 0.65, 0.65)

        when( dir) {
            UP -> g.fillPolygon(
                    logical_y.map { w - (w * it).round }.toIntArray(),
                    logical_x.map { h - (h* it).round }.toIntArray(), 7)
            DOWN -> g.fillPolygon(
                    logical_y.map { (w * it).round }.toIntArray(),
                    logical_x.map { (h* it).round }.toIntArray(), 7)
            LEFT -> g.fillPolygon(
                    logical_x.map { w - (w * it).round }.toIntArray(),
                    logical_y.map { h - (h* it).round }.toIntArray(), 7)
            RIGHT -> g.fillPolygon(
                    logical_x.map { (w * it).round }.toIntArray(),
                    logical_y.map { (h* it).round }.toIntArray(), 7)
        }
    }
}