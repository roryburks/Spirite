package spirite.gui.views.animation.structureView

import sgui.swing.advancedComponents.CrossContainer.CrossLayout
import rb.extendo.dataStructures.SinglySequence
import rb.extendo.extensions.then
import rb.jvm.owl.addWeakObserver
import rb.IContract
import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animation.ffa.IFfaFrame
import spirite.base.imageData.animation.ffa.IFfaLayer
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.util.linear.Rect
import sgui.generic.components.initializers.CrossInitializer
import sgui.generic.components.initializers.CrossRowInitializer
import sgui.generic.components.IComponent
import sgui.generic.components.IComponent.BasicCursor.DEFAULT
import sgui.generic.components.ICrossPanel
import sgui.generic.components.IScrollContainer
import sgui.generic.components.events.MouseEvent
import sgui.swing.skin.Skin
import spirite.gui.views.animation.structureView.ffa.FfaStructBuilderFactory
import spirite.gui.views.animation.structureView.ffa.IAnimDragBehavior
import spirite.gui.views.animation.structureView.ffa.IFFAStructView
import spirite.hybrid.Hybrid
import sgui.swing.components.SJPanel
import sgui.swing.components.SwComponent
import java.awt.Graphics
import java.awt.Graphics2D
import java.lang.ref.WeakReference

private object RememberedStates {
    // Not sure if this is how I want to do this, but I'm fine with it for now.
    data class RememberedState(val expanded: Boolean = false)

    private val map = mutableMapOf<Int,Pair<WeakReference<IFfaLayer>,RememberedState>>()

    fun setState(layer: IFfaLayer, state: RememberedState)
    {
        map.values.removeIf { it.first.get() == null }
        map[layer.hashCode()] = Pair(WeakReference(layer), state)
    }
    fun getState( layer: IFfaLayer) : RememberedState?
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
    var stretchWidth = 0

    var NAME_WIDTH = 60
    var LAYER_HEIGHT = 32
    var SQUISHED_NAME_HEIGHT = 12
    var TICK_WIDTH = 32
    var TICK_HEIGHT = 16

    private val _structBuilderFactory = FfaStructBuilderFactory(master) {rebuild()}


    var viewspace = FFAStructPanelViewspace(
            NAME_WIDTH,
            0,
            TICK_WIDTH,
            emptyMap(),
            emptyMap(),
            emptyMap())

    private var dragBehavior: IAnimDragBehavior? = null

    private data class FrameLinkSet(
            val frameRange: IntRange,
            val frame: IFfaFrame,
            val view: IFFAStructView)

    private data class LayerBuildSet(
            val height: Int,
            val layout: CrossRowInitializer.() -> Unit,
            val frameData: List<FrameLinkSet>,
            val nameComponent: IFFAStructView)

    // region Building
    private fun rebuild() {
        val frameLinks = mutableMapOf<IFfaFrame,IFFAStructView>()
        val layerLinks = mutableMapOf<IFfaLayer, IFFAStructView>()
        val viewMap = mutableMapOf<IFfaLayer,IntRange>()
        var wy = 0

        imp.setLayout {
            val start = anim.start
            val end = anim.end

            // Anim Layers
            anim.layers.forEach {layer ->
                val built = buildLayer(layer, NAME_WIDTH, TICK_WIDTH)
                rows += built.layout
                viewMap[layer] = IntRange(wy, wy+built.height)
                layerLinks[layer] = built.nameComponent
                wy += built.height

                built.frameData.forEach {frameLinks[it.frame] = it.view}
            }

            // Bottom Justification
            rows += {
                addGap(NAME_WIDTH)
                (start until end).forEach { add(TickPanel(it), width = TICK_WIDTH) }
                height = TICK_HEIGHT
            }
        }

        viewspace = FFAStructPanelViewspace(
                NAME_WIDTH,
                0,
                TICK_WIDTH,
                HashMap(viewMap),
                frameLinks,
                layerLinks)
        anim.workspace.groupTree.selectedNode?.also {setBordersForNode(it) }
    }

    private fun buildLayer(
            layer: IFfaLayer,
            nameWidth: Int,
            tickWidth: Int)
            : LayerBuildSet
    {
        val builder = _structBuilderFactory.getFactoryForFfaLayer(layer)

        val nameView = builder.buildNameComponent(layer)

        var met = anim.start
        val end = anim.end

        val frameMap = mutableListOf<FrameLinkSet>()
        while (met < end) {
            val frame = layer.getFrameFromLocalMet(met, false)

            if( frame == null)
                met++
            else {
                val len = frame.length
                if( len != 0) {
                    val frameView = builder.buildFrameComponent(layer, frame)
                    frameMap.add(FrameLinkSet(IntRange(met, met+len), frame, frameView))
                }
                met += len
            }
        }

        val layerHeight = frameMap.asSequence().map { it.view.height }
                .then(SinglySequence(nameView.height))
                .max() ?: 0

        val initializer : CrossRowInitializer.() -> Unit = {
            add(nameView.component, width = nameWidth, height = nameView.height)
            frameMap.forEach { add(it.view.component, width = tickWidth * (it.frameRange.last - it.frameRange.first), height = it.view.height) }

            height = layerHeight
        }

        return LayerBuildSet(
                layerHeight,
                initializer,
                frameMap,
                nameView)
    }
    // endregion

    private inner class TickPanel( val tick: Int, private val imp: ICrossPanel = Hybrid.ui.CrossPanel())
        : ICrossPanel by imp
    {
        init {
            imp.setLayout {
                rows.add(Hybrid.ui.Label("$tick"))
            }
        }
    }

    // region Listener/Observer Bindings
    private val _animationStructureObserverK = anim.workspace.animationManager.animationStructureChangeObservable.addWeakObserver(
        object : AnimationStructureChangeObserver {
            override fun animationStructureChanged(animation: Animation) {
                if( animation == anim)
                    rebuild()
            }
        }
    )

    private  var _activePartK : IContract? = null
    private val _selectedNodeK = master.centralObservatory.selectedNode.addWeakObserver { new, old ->
        _activePartK?.void()
        if( old != null) {
            // TODO
        }
        if( new != null)
            setBordersForNode(new)
    }

    private fun setBordersForNode( node: Node){
        // TODO
//        frameLinks.lookup(node).forEach { it.component.setColoredBorder(Colors.BLACK, 2) }
//        val spriteLayer = ((node as? LayerNode)?.layer as? SpriteLayer)
//        if( spriteLayer != null) {
//            _activePartK = spriteLayer.activePartBind.addWeakObserver { new, _ ->
//                partLinks.values.flatten().forEach { it.component.setBasicBorder(null) }
//                if( new != null)
//                    partLinks.lookup(new).forEach { it.component.setColoredBorder(Colors.BLACK, 2) }
//            }
//        }
    }

    // endregion


    init /* Behavior  */ {
        imp.background = Skin.Global.BgDark.jcolor
        rebuild()

        val nullingContract = object : IContract {
            override fun void() { dragBehavior = null}
        }

        fun interpretEvt( evt: MouseEvent) {
            if( dragBehavior == null) {
                val pt = evt.point.convert(this)

                val brain = viewspace.getViewFromCoordinates(pt.x, pt.y)?.dragBrain

                if( brain == null)
                {
                    setBasicCursor(DEFAULT)
                    return
                }

                dragBehavior = brain.interpretMouseEvent(evt, this)
            }

            dragBehavior?.interpretMouseEvent(evt, nullingContract)
        }

        onMouseClick += {interpretEvt(it)}
        onMouseDrag += {interpretEvt(it)}
        onMouseEnter += {interpretEvt(it)}
        onMouseExit += {interpretEvt(it)}
        onMouseMove += {interpretEvt(it)}
        onMousePress += {interpretEvt(it)}
        onMouseRelease += {interpretEvt(it)}
    }


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
            context.dragBehavior?.draw(g)
        }
    }
}

data class FFAStructPanelViewspace(
        val leftJustification: Int,
        val topJustification: Int,
        val tickWidth: Int,
        val layerHeights: Map<IFfaLayer,IntRange>,
        val frameMap: Map<IFfaFrame,IFFAStructView>,
        val layerMap: Map<IFfaLayer, IFFAStructView>)
{
    fun rectForRangeInLayer(layer: IFfaLayer, range: IntRange) : Rect
    {
        val heightRange = layerHeights[layer]
        return Rect(
                leftJustification + tickWidth*range.first, heightRange?.first?:0,
                tickWidth*(range.last-range.first), heightRange?.run { last-first } ?:0)
    }

    fun getViewFromCoordinates( x: Int, y: Int) : IFFAStructView?
    {
        if( y < topJustification) return null
        val layer = layerHeights.entries.asSequence()
                .firstOrNull { it.value.contains(y - topJustification) }
                ?.key ?: return null

        if( x < leftJustification)
            return layerMap[layer]
        val met = (x - leftJustification) / tickWidth
        val frame = layer.getFrameFromLocalMet(met) ?: return null

        return frameMap[frame]
    }
}