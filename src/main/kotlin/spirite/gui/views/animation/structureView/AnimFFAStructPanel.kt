package spirite.gui.views.animation.structureView

import CrossLayout
import rb.extendo.extensions.append
import rb.extendo.extensions.lookup
import rb.jvm.owl.addWeakObserver
import rb.owl.IContract
import rb.vectrix.mathUtil.i
import rb.vectrix.mathUtil.round
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FFALayer
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.base.imageData.animation.ffa.FFALayerLexical
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.util.ColorARGB32Normal
import spirite.base.util.Colors
import spirite.base.util.linear.Rect
import spirite.gui.Direction
import spirite.gui.Direction.*
import spirite.gui.components.advanced.crossContainer.CrossInitializer
import spirite.gui.components.advanced.crossContainer.CrossRowInitializer
import spirite.gui.components.basic.IButton
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.IComponent.BasicCursor.DEFAULT
import spirite.gui.components.basic.IComponent.BasicCursor.E_RESIZE
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.IScrollContainer
import spirite.gui.components.basic.events.MouseEvent.MouseButton
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.gui.resources.Skin
import spirite.gui.resources.SwIcons
import spirite.gui.views.animation.structureView.AnimDragStateManager.ResizingFrameBehavior
import spirite.gui.views.animation.structureView.RememberedStates.RememberedState
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.JColor
import spirite.pc.gui.basic.SwComponent
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.lang.ref.WeakReference
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

private object RememberedStates {
    // Not sure if this is how I want to do this, but I'm fine with it for now.
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


    lateinit var scrollContext : IScrollContainer
    var nameWidth = 60
    var layerHeight = 32

    var squishedNameHeight = 12

    var tickWidth = 32
    var tickHeight = 16

    var viewspace = FFAStructPanelViewspace(
            nameWidth,
            0,
            tickWidth,
            emptyMap(),
            nameWidth)

    private val frameLinks = mutableMapOf<Node,MutableList<IComponent>>()
    private val partLinks = mutableMapOf<SpritePart,MutableList<IComponent>>()

    private fun rebuild() {
        frameLinks.clear()
        partLinks.clear()

        val viewMap = mutableMapOf<FFALayer,IntRange>()
        var wy = 0

        imp.setLayout {
            val start = anim.start
            val end = anim.end

            anim.layers.forEach {layer ->
                val built = buildLayerInfo(layer)
                rows += buildLayerInfo(layer).second
                viewMap[layer] = IntRange(wy, wy+built.first)
                wy += built.first
            }

            // Bottom Justification
            rows += {
                addGap(nameWidth)
                (start until end).forEach { add(TickPanel(it), width = tickWidth) }
                height = tickHeight
            }
        }

        viewspace = FFAStructPanelViewspace(nameWidth, 0, tickWidth, HashMap(viewMap), nameWidth + tickWidth * anim.end)
        anim.workspace.groupTree.selectedNode?.also {setBordersForNode(it) }
    }

    var stretchWidth = 0

    private fun buildLayerInfo(layer: FFALayer) : Pair<Int,CrossRowInitializer.() -> Unit>{
        val state = RememberedStates.getState(layer)
        val distinctNames = layer.frames.flatMap {frame ->
            ((frame.node as? LayerNode)?.layer as? SpriteLayer)?.parts?.map { it.partName } ?: listOf<String?>(null)
        }.distinct()
        val distinctCount = distinctNames.count()
        val expandable = distinctCount > 1

        val maxSubLoopDepth = layer.frames
                .filter { it.marker == START_LOCAL_LOOP && it.length > 0 }
                .map { it.loopDepth }
                .max() ?: 0

        fun defaultBuild(layer: FFALayer) : CrossRowInitializer.() -> Unit = {

            // Label
            if(expandable)addFlatGroup(nameWidth-12) {
                val expandButton = Hybrid.ui.Button()
                expandButton.background = Colors.TRANSPARENT
                expandButton.opaque = false
                expandButton.setBasicBorder(null)
                expandButton.setIcon(SwIcons.SmallIcons.Rig_New)
                expandButton.action = {
                    RememberedStates.setState(layer, RememberedState(true))
                    rebuild()
                }
                add(expandButton,12,12)
            }
            add(NamePanel(layer), width = nameWidth )

            // Frames
            val frames = layer.frames.toList()
            fun subBuildFrame( start: Int, length: Int, context: CrossRowInitializer) : Int
            {
                var index = start
                var caret = 0
                var localLength = 0

                while (index < frames.size) {
                    val frame = frames[index++]
                    val effectiveLen = min(frame.length, length-caret)
                    caret += frame.length
                    localLength += frame.length


                    when( frame.marker) {
                        FRAME -> {
                            if (frame.length > 0) {
                                val component = FramePanel(frame)
                                frame.node?.also { frameLinks.append(it, component)}
                                context.add(component, width = tickWidth * effectiveLen, height = layerHeight)
                            }
                        }
                        GAP -> {
                            val component = GapPanel(frame)
                            frame.node?.also { frameLinks.append(it, component)}
                            context.add(component, width = tickWidth * effectiveLen, height = layerHeight)
                        }
                        START_LOCAL_LOOP -> {
                            context.add(Hybrid.ui.CrossPanel {
                                rows.add(LocalLoopPanel(frame), width = tickWidth*effectiveLen, height = squishedNameHeight)

                                rows += {
                                    if (frame.length != 0)
                                        subBuildFrame(index, effectiveLen, this)
                                    height = layerHeight + squishedNameHeight * (maxSubLoopDepth - frame.loopDepth)
                                }
                            }.also { it.markAsPassThrough() })
                            var inll = 1
                            while( inll > 0) when(frames[index++].marker) {
                                END_LOCAL_LOOP -> inll--
                                START_LOCAL_LOOP -> inll++
                            }
                        }
                        END_LOCAL_LOOP -> {
                            return caret
                            //if( localLength == 0) return caret
                            //index = start
                        }
                    }

                    if( caret == length) return caret
                }

                return caret
            }


            val end = anim.end
            val caret = subBuildFrame(0, end, this)
            if( caret < end - 1) {
                add(BlankPanel(), width = (end - caret) * tickWidth)
            }

            height = layerHeight + squishedNameHeight*(maxSubLoopDepth)
        }

        fun expandedBuild(layer: FFALayer) : CrossRowInitializer.()-> Unit = {
            this.addFlatGroup(nameWidth-12) {
                val expandButton = Hybrid.ui.Button()
                expandButton.background = Colors.TRANSPARENT
                expandButton.opaque = false
                expandButton.setBasicBorder(null)
                expandButton.setIcon(SwIcons.SmallIcons.Rig_Remove)
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
                                val topBar = FramePanel(frame)
                                frame.node?.also { frameLinks.append(it, topBar)}
                                add( topBar, height = squishedNameHeight)

                                val lnLayer = (frame.node as? LayerNode)?.layer
                                distinctNames.forEach {name ->
                                    add( when(lnLayer) {
                                        null -> BlankPanel()
                                        is SpriteLayer -> {
                                            val part = lnLayer.parts.firstOrNull() { it.partName == name }
                                            when( part) {
                                                null -> BlankPanel()
                                                else -> {
                                                    val comp = SpritePartPanel(part, frame)
                                                    partLinks.append(part, comp)
                                                    comp
                                                }
                                            }
                                        }
                                        else -> when( name) {
                                            null -> {
                                                val comp = FramePanel(frame)
                                                frame.node?.also { frameLinks.append(it, comp)}
                                                comp
                                            }
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


        return when {
            state == null || !state.expanded || !expandable -> Pair(layerHeight, defaultBuild(layer))
            else -> Pair(squishedNameHeight + layerHeight * distinctCount, expandedBuild(layer))
        }
    }


    private open inner class FrameResizeable(
            private val _imp: IComponent,
            private val _frame: FFAFrame)
    {
        init {
            _imp.markAsPassThrough()
            _imp.onMouseMove += { evt ->
                if (evt.point.x > _imp.width - 3)
                    _imp.setBasicCursor(E_RESIZE)
                else
                    _imp.setBasicCursor(DEFAULT)
            }
            _imp.onMousePress += {evt ->
                if (evt.point.x > _imp.width - 3) {
                    dragStateManager.behavior = ResizingFrameBehavior(_frame, dragStateManager, viewspace)
                    redraw()
                }
            }
        }
    }

    // region SubPanels
    private inner class NamePanel(val layer: FFALayer, private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        init {
            imp.setBasicBorder(BEVELED_LOWERED)

            val label = Hybrid.ui.EditableLabel("layer")
            if( layer is FFALayerLexical) {
                imp.setLayout {
                    rows.add(label)
                    rows.add(LexiconButton(layer))
                }
            }
            else {
                imp.setLayout { rows.add(label) }
            }
            imp.onMouseRelease += { label.requestFocus() }
        }

    }
    private inner class LexiconButton(val layer: FFALayerLexical, private val imp: IButton = Hybrid.ui.Button("Lexicon"))
        : IComponent by imp
    {
        init {
            imp.onMouseClick += {redoLexicon()}
        }

        fun redoLexicon() {
            val lexicon = master.dialog.promptForString("Enter new Lexicon:",layer.lexicon) ?: return
            layer.lexicon = lexicon
        }
    }

    private inner class SubNamePanel(val title: String, private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        init {
            imp.setBasicBorder(BEVELED_LOWERED)
            val label = Hybrid.ui.EditableLabel(title)
            imp.setLayout { rows.add(label) }
            imp.onMouseRelease += {label.requestFocus()}
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

    private inner class GapPanel(
            val frame: FFAFrame,
            private val imp : IComponent = SwComponent(DashedOutPanel(null, Skin.Global.Fg.jcolor)))
        : FrameResizeable(imp,frame),IComponent by imp
    {
    }

    private inner class BlankPanel() : IComponent by SwComponent(DashedOutPanel(null, Skin.Global.Bg.jcolor))
    private inner class PseudoBlankPanel(
            val frame: FFAFrame,
            private val imp : IComponent = SwComponent(DashedOutPanel(null, Skin.Global.Bg.jcolor)))
        : FrameResizeable(imp,frame), IComponent by imp

    private inner class LocalLoopPanel(
            val frame: FFAFrame,
            private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : FrameResizeable(imp,frame), IComponent by imp
    {
        init {
            background = when(frame.loopDepth % 4) {
                0 -> ColorARGB32Normal(0xffab93f2.i)
                1 -> ColorARGB32Normal(0xff92c7f1.i)
                2 ->ColorARGB32Normal(0xfff0c491.i)
                else -> ColorARGB32Normal(0xffc1f2ab.i)
            }

            imp.setLayout { rows.add(Hybrid.ui.Label(frame.node?.name ?: "")) }
        }
    }

    private inner class FramePanel(
            val frame: FFAFrame,
            private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : FrameResizeable(imp,frame), IComponent by imp
    {
        val imageBox = Hybrid.ui.ImageBox(ImageBI(BufferedImage(1,1,BufferedImage.TYPE_4BYTE_ABGR)))
        init {
            imp.ref = this
            imp.opaque = false
            imp.setLayout {
                cols.add(imageBox , width = tickWidth)

                if( frame.length > 1) {
                    cols.add(SwComponent(ArrowPanel(null, Skin.FFAAnimation.Arrow.jcolor, RIGHT)), width = tickWidth * (frame.length-1))
                }

                cols.flex = 1f
            }

            imp.onMousePress += {evt ->
                when(evt.button) {
                    MouseButton.RIGHT -> {
                        master.contextMenus.LaunchContextMenu(
                                bottomRight,
                                listOf(
                                        MenuItem("Add Gap &Before", customAction = {frame.layer.addGapFrameAfter(frame.previous)}),
                                        MenuItem("Add Gap &After", customAction = {frame.layer.addGapFrameAfter(frame)}),
                                        MenuItem("Increase &Length", customAction = {frame.length += 1}) )
                        )
                    }
                    MouseButton.LEFT -> {
                        val tree = frame.layer.context.workspace.groupTree
                        frame.node?.also { tree.selectedNode = it}
                    }
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
        :FrameResizeable(imp,frame), IComponent by imp
    {
        val imageBox = Hybrid.ui.ImageBox()
        init {
            imp.opaque = false
            imp.ref = this
            imp.setLayout {
                cols.add(imageBox , width = tickWidth)

                if( frame.length > 1) {
                    cols.add(SwComponent(ArrowPanel(null, Skin.FFAAnimation.Arrow.jcolor, RIGHT)), width = tickWidth * (frame.length-1))
                }

                cols.flex = 1f
            }

            imp.onMouseRelease += {evt ->
                when( evt.button) {
                    MouseButton.RIGHT -> {
                        master.contextMenus.LaunchContextMenu(
                                bottomRight,
                                listOf(
                                        MenuItem("Add Gap &Before", customAction = {frame.layer.addGapFrameAfter(frame.previous)}),
                                        MenuItem("Add Gap &After", customAction = {frame.layer.addGapFrameAfter(frame)}),
                                        MenuItem("Increase &Length", customAction = {frame.length += 1}) )
                        )
                    }
                    MouseButton.LEFT -> {
                        part.context.activePart = part
                        val tree = part.context.workspace.groupTree
                        frame.node?.also { tree.selectedNode = it}
                    }
                    else ->{}
                }
            }
        }

        val xyz = master.nativeThumbnailStore.contractThumbnail(part, anim.workspace) {
            imageBox.setImage(it)
        }
    }
    // endregion

    // region Listener/Observer Bindings
    private val _animationStructureObserverK = anim.workspace.animationManager.animationStructureChangeObservable.addWeakObserver(
        object : AnimationStructureChangeObserver {
            override fun animationStructureChanged(animation: Animation) {
                if( animation == anim)
                    rebuild()
            }
        }
    )

    private  var _activePartContract : IContract? = null
    private val selectedNodeK = master.centralObservatory.selectedNode.addWeakObserver { new, old ->
        _activePartContract?.void()
        if( old != null) {
            frameLinks.lookup(old).forEach { it.setBasicBorder(null) }
            partLinks.values.flatten().forEach { it.setBasicBorder(null) }
        }
        if( new != null)
            setBordersForNode(new)
    }

    private fun setBordersForNode( node: Node){
        frameLinks.lookup(node).forEach { it.setColoredBorder(Colors.BLACK, 2) }
        val spriteLayer = ((node as? LayerNode)?.layer as? SpriteLayer)
        if( spriteLayer != null) {
            _activePartContract = spriteLayer.activePartBind.addWeakObserver { new, _ ->
                partLinks.values.flatten().forEach { it.setBasicBorder(null) }
                if( new != null)
                    partLinks.lookup(new).forEach { it.setColoredBorder(Colors.BLACK, 2) }
            }
        }
    }

    // endregion

    init /* Bindings */ {
        imp.background = Skin.Global.BgDark.jcolor
        rebuild()

        onMouseRelease += {
            val pt = it.point.convert(this)
            dragStateManager.behavior?.release(pt.x, pt.y)
        }
        onMouseDrag += {
            val pt = it.point.convert(this)
            dragStateManager.behavior?.move(pt.x, pt.y)
        }
    }

    internal val dragStateManager = AnimDragStateManager(this)
}

data class FFAStructPanelViewspace(
        val leftJustification: Int,
        val topJustification: Int,
        val tickWidth: Int,
        val layerHeights: Map<FFALayer,IntRange>,
        val naturalWidth: Int)
{
    fun rectForRangeInLayer( layer: FFALayer, range: IntRange) : Rect
    {
        val heightRange = layerHeights[layer]
        return Rect(
                leftJustification + tickWidth*range.first, heightRange?.first?:0,
                tickWidth*(range.last-range.first), heightRange?.run { last-first } ?:0)
    }
}

internal class AnimDragStateManager(val context: AnimFFAStructPanel)
{
    var behavior: IAnimStateBehavior? = null
        set(value) {field = value; context.redraw()}

    interface IAnimStateBehavior {
        fun move(x: Int, y: Int)
        fun release(x: Int, y: Int)
        fun draw( gc: Graphics2D)   // TODO: This really should be GraphicsContext, but right now we haven't re-implemented AWTGraphicsContext
    }


    class ResizingFrameBehavior(
            val frame: FFAFrame,
            val context: AnimDragStateManager,
            val viewspace: FFAStructPanelViewspace)
        : IAnimStateBehavior
    {
        val start = frame.start
        var len = frame.length

        override fun move(x: Int, y: Int) {
            len = max(0, (x - viewspace.leftJustification + viewspace.tickWidth/2) / viewspace.tickWidth - start)
            val endBox = viewspace.rectForRangeInLayer(frame.layer, IntRange(start+len-1, start+len))
            context.context.stretchWidth = endBox.x2
            context.context.scrollContext.makeAreaVisible(endBox)
            context.context.redraw()
        }

        override fun release(x: Int, y: Int) {
            frame.length = len
            context.behavior = null
        }

        override fun draw(gc: Graphics2D) {
            val range = viewspace.layerHeights[frame.layer]?: return
            val x = (start + len)* viewspace.tickWidth + viewspace.leftJustification
            gc.stroke = BasicStroke(2f)
            gc.color = Color.BLACK
            gc.drawLine(x, range.first, x, range.last)
        }
    }
}

// region Custom Panels
private class AnimFFAStructPanelImp : JPanel() {
    lateinit var context : AnimFFAStructPanel

    fun setLayout( constructor: CrossInitializer.()->Unit) {
        removeAll()
        val list = mutableListOf<IComponent>()
        layout = CrossLayout.buildCrossLayout(this, list, constructor)
    }

    init {
    }

    override fun paint(g: Graphics) {
        val g2 = (g as Graphics2D)
        g2.color = background
        g2.fillRect(0,0,width,height)
        drawBackground(g2)
        super.paintChildren(g2)
        drawForeground(g2)
    }

    private fun drawBackground(g: Graphics2D) {

    }

    private fun drawForeground( g: Graphics2D) {
        context.dragStateManager.behavior?.draw(g)
    }
}

private class DashedOutPanel(val bgcol: JColor?, val fgcol: JColor) : JPanel() {
    init {
        background = null
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        if( bgcol != null) {
            g.color = bgcol
            g.fillRect(0, 0, width, height)
        }

        g.color = fgcol
        (0.. (width + height)/4)
                .forEach { g.drawLine(0, it*4, it*4, 0)}

    }
}
private class ArrowPanel(val bgcol: JColor?, val fgcol: JColor, val dir: Direction) : JPanel() {
    init {
        background = null
        isOpaque = false
    }
    override fun paintComponent(g: Graphics) {
        if( bgcol != null) {
            g.color = bgcol
            g.fillRect(0, 0, width, height)
        }

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
// endregion