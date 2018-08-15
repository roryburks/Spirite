package spirite.gui.components.major.animation.structureView

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.util.Colors
import spirite.base.util.groupExtensions.mapAggregated
import spirite.base.util.round
import spirite.gui.Direction
import spirite.gui.Direction.*
import spirite.gui.components.advanced.crossContainer.CrossInitializer
import spirite.gui.components.advanced.crossContainer.CrossRowInitializer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.events.MouseEvent.MouseButton
import spirite.gui.components.major.animation.structureView.RememberedStates.RememberedState
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.gui.resources.Skin
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.JColor
import spirite.pc.gui.basic.SwComponent
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.lang.ref.WeakReference
import javax.swing.JPanel

private object RememberedStates {
    data class RememberedState(val expanded: Boolean = false)

    private val map = mutableMapOf<Int,Pair<WeakReference<FFALayer>,RememberedState>>()

    fun setState( layer: FFALayer, state: RememberedState)
    {
        map.values.removeIf { it.first.get() == null }
        map[layer.hashCode()] = Pair(WeakReference(layer), state)
    }
    fun getState( layer: FFALayer) : RememberedState?
    {
        map.values.removeIf { it.first.get() == null }
        val maybeMatch = map[layer.hashCode()] ?: return null
        if( maybeMatch.first.get() != layer) return null

        return maybeMatch.second
    }
}

class AnimFFAStructPanel
private constructor(
        val master: IMasterControl,
        val anim: FixedFrameAnimation,
        private val imp: AnimFFAStructPanelImp)
    : IComponent by SwComponent(imp)
{
    constructor(master: IMasterControl, anim: FixedFrameAnimation) : this(master, anim, AnimFFAStructPanelImp())
    init {imp.context = this}

    var nameWidth = 60
    var layerHeight = 24

    var squishedNameHeight = 12

    var tickWidth = 24
    var tickHeight = 16

    private val frameLinks = mutableListOf<FramePanel>()

    fun rebuild() {
        frameLinks.clear()

        imp.setLayout {
            val start = anim.start
            val end = anim.end

            anim.layers.forEach {layer -> rows += buildLayerInfo(layer) }

            rows += {
                addGap(nameWidth)
                (start until end).forEach { add(TickPanel(it), width = tickWidth) }
                height = tickHeight
            }
        }
    }

    fun buildLayerInfo(layer: FFALayer) : CrossRowInitializer.() -> Unit{
        val state = RememberedStates.getState(layer)
        val distinctNames = layer.frames.mapAggregated {frame ->
            ((frame.node as? LayerNode)?.layer as? SpriteLayer)?.parts?.map { it.partName } ?: listOf<String?>(null)
        }.distinct()
        val expandable = distinctNames.count() > 1


        fun defaultBuild(layer: FFALayer) : CrossRowInitializer.() -> Unit = {
            if(expandable)addFlatGroup(nameWidth-12) {
                addGap(layerHeight-12)
                val expandButton = Hybrid.ui.Button()
                expandButton.background = Colors.TRANSPARENT
                expandButton.opaque = false
                expandButton.setBasicBorder(null)
                expandButton.setIcon(SwIcons.SmallIcons.Rig_Remove)
                expandButton.action = {
                    RememberedStates.setState(layer, RememberedState(true))
                    rebuild()
                }
                add(expandButton,12,12)
            }
            add(NamePanel(layer), width = nameWidth )

            var len = 0
            for( frame in layer.frames) {
                len += frame.length
                when( frame.marker) {
                    FRAME -> {
                        if (frame.length > 0)
                            add( FramePanel(frame).also {frameLinks.add(it) }, width = tickWidth * frame.length)
                    }
                    GAP -> {
                        add(GapPanel(frame), width = tickWidth * frame.length)
                    }
                    START_LOCAL_LOOP -> {}
                    END_LOCAL_LOOP -> {}
                }
            }

            val end = anim.end
            if( len < end) {
                add(BlankPanel(), width = (end - len) * tickWidth)
            }

            height = layerHeight
        }

        fun expandedBuild(layer: FFALayer) : CrossRowInitializer.()-> Unit = {
            this.addFlatGroup(nameWidth-12) {
                val expandButton = Hybrid.ui.Button()
                expandButton.background = Colors.TRANSPARENT
                expandButton.opaque = false
                expandButton.setBasicBorder(null)
                expandButton.setIcon(SwIcons.SmallIcons.Rig_New)
                expandButton.action = {
                    RememberedStates.setState(layer, RememberedState(false))
                    rebuild()
                }
                add(expandButton,12,12)
            }

            // Labels
            this += {
                add(NamePanel(layer), width = nameWidth, height = squishedNameHeight)
                distinctNames.forEach {
                    add(SubNamePanel(it?:"<base>"), width = nameWidth, height = layerHeight)
                }
                width = nameWidth
            }
            var len = 0

            // Frame Content
            for( frame in layer.frames) {
                len += frame.length
                when( frame.marker) {
                    FRAME -> {
                        if (frame.length > 0) {
                            this += {
                                add( FramePanel(frame).also {frameLinks.add(it) }, height = squishedNameHeight)
                                val lnLayer = (frame.node as? LayerNode)?.layer
                                distinctNames.forEach {name ->
                                    add( when(lnLayer) {
                                        null -> BlankPanel()
                                        is SpriteLayer -> {
                                            val part = lnLayer.parts.firstOrNull() { it.partName == name }
                                            when( part) {
                                                null -> BlankPanel()
                                                else -> SpritePartPanel(part, frame)
                                            }
                                        }
                                        else -> when( name) {
                                            null -> FramePanel(frame)
                                            else -> BlankPanel()
                                        }
                                    }, height = layerHeight)
                                }

                                width = tickWidth * frame.length
                            }
                        }
                    }
                    GAP -> {
                        this += {
                            add(GapPanel(frame), height = squishedNameHeight)
                            distinctNames.forEach { add(GapPanel(frame), height = layerHeight) }
                            width = tickWidth * frame.length
                        }
                    }
                    START_LOCAL_LOOP -> {}
                    END_LOCAL_LOOP -> {}
                }
            }

            val end = anim.end
            if( len < end) {
                add(BlankPanel(), width = (end - len) * tickWidth)
            }
        }

        // BEGIN YGGDRASIL
        return when {
            state == null || !state.expanded -> defaultBuild(layer)
            else -> {
                if( !expandable) defaultBuild(layer)
                else expandedBuild(layer)
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
    private inner class SubNamePanel(val title: String, private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        init {
            imp.setBasicBorder(BEVELED_LOWERED)
            val label = Hybrid.ui.EditableLabel(title)
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

    private inner class GapPanel(val frame: FFAFrame) : IComponent by SwComponent(DashedOutPanel(Skin.Global.BgDark.jcolor, Skin.Global.Fg.jcolor))
    {
    }

    private inner class BlankPanel() : IComponent by SwComponent(DashedOutPanel(Skin.Global.BgDark.jcolor, Skin.Global.Bg.jcolor))


    private inner class FramePanel(
            val frame: FFAFrame,private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        val imageBox = Hybrid.ui.ImageBox(ImageBI(BufferedImage(1,1,BufferedImage.TYPE_4BYTE_ABGR)))
        init {
            imp.ref = this
            imp.setLayout {
                cols.add(imageBox , width = tickWidth)

                if( frame.length > 1) {
                    cols.add(SwComponent(ArrowPanel(Skin.Global.BgDark.jcolor, Skin.FFAAnimation.Arrow.jcolor, RIGHT)), width = tickWidth * (frame.length-1))
                }

                cols.height = layerHeight
            }

            imp.onMouseRelease = {evt ->
                if( evt.button == MouseButton.RIGHT) {
                    master.contextMenus.LaunchContextMenu(
                            bottomRight,
                            listOf(
                                    MenuItem("Add Gap &Before", customAction = {frame.layer.addGapFrameAfter(frame.previous)}),
                                    MenuItem("Add Gap &After", customAction = {frame.layer.addGapFrameAfter(frame)}),
                                    MenuItem("Increase &Length", customAction = {frame.length += 1}) )
                    )
                }
            }
        }

        val xyz = master.nativeThumbnailStore.contractThumbnail(frame.node!!, anim.workspace) {
            imageBox.setImage(it)
        }
    }

    private inner class SpritePartPanel(
        val part: SpritePart,
        val frame: FFAFrame,
        private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        :IComponent by imp
    {
        val imageBox = Hybrid.ui.ImageBox()
        init {
            imp.ref = this
            imp.setLayout {
                cols.add(imageBox , width = tickWidth)

                if( frame.length > 1) {
                    cols.add(SwComponent(ArrowPanel(Skin.Global.BgDark.jcolor, Skin.FFAAnimation.Arrow.jcolor, RIGHT)), width = tickWidth * (frame.length-1))
                }

                cols.height = layerHeight
            }

            imp.onMouseRelease = {evt ->
                if( evt.button == MouseButton.RIGHT) {
                    master.contextMenus.LaunchContextMenu(
                            bottomRight,
                            listOf(
                                    MenuItem("Add Gap &Before", customAction = {frame.layer.addGapFrameAfter(frame.previous)}),
                                    MenuItem("Add Gap &After", customAction = {frame.layer.addGapFrameAfter(frame)}),
                                    MenuItem("Increase &Length", customAction = {frame.length += 1}) )
                    )
                }
            }
        }

        val xyz = master.nativeThumbnailStore.contractThumbnail(part, anim.workspace) {
            imageBox.setImage(it)
        }
    }

    // region Listener/Observer Bindings
    private val _animationStructureObserver = object : AnimationStructureChangeObserver {
        override fun animationStructureChanged(animation: Animation) {
            if( animation == anim)
                rebuild()
        }
    }.also {anim.workspace.animationManager.animationStructureChangeObservable.addObserver( it)}
    // endregion

    init {
        imp.background = Skin.Global.BgDark.jcolor
        rebuild()
    }
}

private class AnimFFAStructPanelImp : JPanel() {
    lateinit var context : AnimFFAStructPanel

    fun setLayout( constructor: CrossInitializer.()->Unit) {
        removeAll()
        val list = mutableListOf<IComponent>()
        layout = CrossLayout.buildCrossLayout(this, list, constructor)
        validate()
        CrossLayout.buildCrossLayout(this, null, constructor)
    }

    init {
    }

    override fun paint(g: Graphics) {
        g.color = background
        g.fillRect(0,0,width,height)
        super.paintChildren(g)
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