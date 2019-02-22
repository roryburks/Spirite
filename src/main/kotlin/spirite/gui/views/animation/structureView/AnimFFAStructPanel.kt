package spirite.gui.views.animation.structureView

import CrossLayout
import rb.extendo.dataStructures.SinglySequence
import rb.extendo.extensions.append
import rb.extendo.extensions.lookup
import rb.extendo.extensions.then
import rb.jvm.owl.addWeakObserver
import rb.owl.IContract
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.IFFALayer
import spirite.base.imageData.animation.ffa.IFFAFrame
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.util.Colors
import spirite.base.util.linear.Rect
import spirite.gui.components.advanced.crossContainer.CrossInitializer
import spirite.gui.components.advanced.crossContainer.CrossRowInitializer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.IScrollContainer
import spirite.gui.resources.Skin
import spirite.gui.resources.SwIcons
import spirite.gui.views.animation.structureView.RememberedStates.RememberedState
import spirite.gui.views.animation.structureView.ffa.FfaStructBuilderFactory
import spirite.gui.views.animation.structureView.ffa.IFFAStructView
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SJPanel
import spirite.pc.gui.basic.SwComponent
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

private object RememberedStates {
    // Not sure if this is how I want to do this, but I'm fine with it for now.
    data class RememberedState(val expanded: Boolean = false)

    private val map = mutableMapOf<Int,Pair<WeakReference<IFFALayer>,RememberedState>>()

    fun setState( layer: IFFALayer, state: RememberedState)
    {
        map.values.removeIf { it.first.get() == null }
        map[layer.hashCode()] = Pair(WeakReference(layer), state)
    }
    fun getState( layer: IFFALayer) : RememberedState?
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

        val viewMap = mutableMapOf<IFFALayer,IntRange>()
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

    private val _structBuilderFactory = FfaStructBuilderFactory(master)

    private fun buildLayer(layer: IFFALayer) {
        val builder = _structBuilderFactory.GetFactoryForFfaLayer(layer)

        val nameView = builder.buildNameComponent(layer)

        var met = anim.start
        val end = anim.end

        val frameMap = mutableListOf<Pair<IntRange,IFFAStructView>>()
        while (met < end) {
            val frame = layer.getFrameFromLocalMet(met, false)

            if( frame == null)
                met++
            else {
                val len = frame.length
                if( len != 0) {
                    val frameView = builder.buildFrameComponent(layer, frame)
                    frameMap.add(Pair(IntRange(met,met+len), frameView))
                }
                met += len
            }
        }

        val layerHeight = frameMap.asSequence().map { it.second.height }
                .then(SinglySequence(nameView.height))
                .max() ?: 0

    }

    private fun buildLayerInfo(layer: IFFALayer) : Pair<Int,CrossRowInitializer.() -> Unit>{
        val state = RememberedStates.getState(layer)
        val distinctNames = layer.frames.flatMap {frame ->
            ((frame.structure.node as? LayerNode)?.layer as? SpriteLayer)?.parts?.map { it.partName } ?: listOf<String?>(null)
        }.distinct()
        val distinctCount = distinctNames.count()
        val expandable = distinctCount > 1

        val maxSubLoopDepth = layer.frames
                .filter { it.marker == START_LOCAL_LOOP && it.length > 0 }
                .map { it.loopDepth }
                .max() ?: 0

        fun defaultBuild(layer: IFFALayer) : CrossRowInitializer.() -> Unit = {

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

        fun expandedBuild(layer: IFFALayer) : CrossRowInitializer.()-> Unit = {
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
                                frame.structure.node?.also { frameLinks.append(it, topBar)}
                                add( topBar, height = squishedNameHeight)

                                val lnLayer = (frame.structure.node as? LayerNode)?.layer
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
                                                frame.structure.node?.also { frameLinks.append(it, comp)}
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

    private inner class TickPanel( val tick: Int, private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        :ICrossPanel by imp
    {
        init {
            imp.setLayout {
                rows.add(Hybrid.ui.Label("$tick"))
            }
        }
    }

//    private open inner class FrameResizeable(
//            private val _imp: IComponent,
//            private val _frame: IFFAFrame)
//    {
//        init {
//            _imp.markAsPassThrough()
//            _imp.onMouseMove += { evt ->
//                if (evt.point.x > _imp.width - 3)
//                    _imp.setBasicCursor(E_RESIZE)
//                else
//                    _imp.setBasicCursor(DEFAULT)
//            }
//            _imp.onMousePress += {evt ->
//                if (evt.point.x > _imp.width - 3) {
//                    dragStateManager.behavior = ResizingFrameBehavior(_frame, dragStateManager, viewspace)
//                    redraw()
//                }
//            }
//        }
//    }

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
        val layerHeights: Map<IFFALayer,IntRange>,
        val naturalWidth: Int)
{
    fun rectForRangeInLayer( layer: IFFALayer, range: IntRange) : Rect
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
            val frame: IFFAFrame,
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
private class AnimFFAStructPanelImp : SJPanel() {
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


// endregion