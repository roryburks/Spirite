package spirite.base.imageData.animation.ffa

import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.MediumBasedAnimation
import spirite.base.imageData.animation.ffa.FFALayerGroupLinked.UnlinkedFrameCluster
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.undo.NullAction

class FixedFrameAnimation(name: String, workspace: IImageWorkspace)
    : MediumBasedAnimation(name, workspace)
{
    constructor(name: String, workspace: IImageWorkspace, node : GroupNode) : this(name, workspace){
        addLinkedLayer(node, true)
    }
    constructor(name: String, workspace: IImageWorkspace, layers: List<IFfaLayer>) : this(name, workspace){
        _layers.addAll(layers)
    }

    private val _layers = mutableListOf<IFfaLayer>()
    val layers : List<IFfaLayer> get() = _layers

    val start : Int get() = _layers.map { it.start }.min() ?: 0
    val end : Int get() = _layers.map { it.end }.max() ?: 0
    override val startFrame: Float get() = start.f
    override val endFrame: Float get() = end.f

    override fun getDrawList(t: Float): List<TransformedHandle> {
        val _t = t.floor
        val met = MathUtil.cycle(start, end, _t)

        val drawList = _layers
                .filter { it.frames.any() }
                .flatMap { layer ->
                    val localMet = if( layer.asynchronous) MathUtil.cycle(layer.start, layer.end, _t) else met
                    layer.getFrameFromLocalMet(localMet)?.getDrawList()?: emptyList()
                }

        return drawList
    }

    class FFAUpdateContract(val changedNodes: Set<Node>)
    {
        val ancestors by lazy {changedNodes.flatMap { it.ancestors}.union(changedNodes)}
    }
    fun treeChanged( changedNodes : Set<Node>) {
        // Remove All Layers referencing nonexistent Groups
//        val toRemove = _layers.asSequence()
//                .mapNotNull { Pair(when(it) {
//                    is FfaLayerLexical -> it.groupLink
//                    is FFALayerGroupLinked -> it.groupLink
//                    else -> return@mapNotNull null
//                }, it) }
//                .map { it.second }
//        _layers.removeAll(toRemove)
//        toRemove.forEach { triggerFFAChange(it) }


        val contract = FFAUpdateContract(changedNodes)

        _layers.asSequence()
                .filterIsInstance<IFFALayerLinked>()
                .filter { it.shouldUpdate(contract) }
                .forEach { it.groupLinkUpdated() }
    }

    internal fun triggerFFAChange( layer: IFfaLayer?) {
        triggerStructureChange()
    }

    fun removeLayer( layer: IFfaLayer) {
        val spot = _layers.indexOf(layer)
        if( spot == -1) return

        workspace.undoEngine.performAndStore(object : NullAction() {
            override val description: String get() = "Remove Layer"
            override fun performAction() {
                _layers.remove(layer)
                triggerFFAChange(layer)
            }
            override fun undoAction() {
                _layers.add(spot, layer)
                triggerFFAChange(layer)
            }
        })
    }

    fun addLinkedLayer(
            group: GroupNode,
            includeSubtrees: Boolean,
            frameMap: Map<Node, FFAFrameStructure>? = null,
            unlinkedClusters: List<UnlinkedFrameCluster>? = null)
    {
        val layer = FFALayerGroupLinked(this, group, includeSubtrees, frameMap, unlinkedClusters)
        addLayer(layer)
    }

    fun addLexicalLayer(group: GroupNode, lexicon: String = "", map: Map<Char,Node>? = null)
    {
        val existingMap = _layers.asSequence()
                .filterIsInstance<FfaLayerLexical>()
                .filter { it.groupLink == group }
                .firstOrNull()?.sharedExplicitMap
        val mapToUse = existingMap ?: (map?.toMutableMap()) ?: mutableMapOf()
        val layer = FfaLayerLexical(this, group, lexicon, mapToUse)
        addLayer(layer)
    }

    fun addCascadingLayer( group: GroupNode) {
        addLayer( FfaLayerCascading(this, group))
    }

    fun addLayer( layer: IFfaLayer) {
        if( layer.anim != this) throw IllegalArgumentException("Frames have to be hard linked to their Animation")
        val spot = _layers.size

        workspace.undoEngine.performAndStore( object : NullAction() {
            override val description: String get() = "Added Layer to Fixed Frame Animation"
            override fun performAction() {
                _layers.add(spot, layer)
                triggerFFAChange(layer)
            }
            override fun undoAction() {
                _layers.remove(layer)
                triggerFFAChange(layer)
            }
        })
    }


    override fun dupe(): Animation {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

object FFALayerCache {

}