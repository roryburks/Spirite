package spirite.base.imageData.animation.ffa

import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import spirite.base.imageData.TransformedHandle
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.MediumBasedAnimation
import spirite.base.imageData.animation.ffa.FfaLayerGroupLinked.UnlinkedFrameCluster
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.undo.NullAction

class FFAUpdateContract(val changedNodes: Set<Node>)
{
    val ancestors by lazy {changedNodes.flatMap { it.ancestors}.union(changedNodes)}
}

class FixedFrameAnimation(name: String, workspace: IImageWorkspace)
    : MediumBasedAnimation(name, workspace)
{

    constructor(name: String, workspace: IImageWorkspace, node : GroupNode) : this(name, workspace){
        addLinkedLayer(node, true)
    }
    constructor(name: String, workspace: IImageWorkspace, layerBuilders: List<(FixedFrameAnimation)->IFfaLayer>) : this(name, workspace){
        _layers.addAll(layerBuilders.map { it(this) })
    }

    // Model
    private val _layers = mutableListOf<IFfaLayer>()
    //

    val layers : List<IFfaLayer> get() = _layers
    val start : Int get() = _layers.map { it.start }.min() ?: 0
    val end : Int get() = _layers.map { it.end }.max() ?: 0
    override val startFrame: Float get() = start.f
    override val endFrame: Float get() = end.f

    override fun getDrawList(t: Float): List<TransformedHandle> {
        val _t = t.floor
        val met = MathUtil.cycle(start, end, _t)

        return _layers
                .filter { it.frames.any() }
                .flatMap { layer ->
                    val localMet = if( layer.asynchronous) MathUtil.cycle(layer.start, layer.end, _t) else met
                    layer.getFrameFromLocalMet(localMet)?.getDrawList()?: emptyList()
                }
    }

    fun treeChanged( changedNodes : Set<Node>) {
        // Remove All Layers referencing nonexistent Groups
//        val toRemove = _layers.asSequence()
//                .mapNotNull { Pair(when(it) {
//                    is FfaLayerLexical -> it.groupLink
//                    is FfaLayerGroupLinked -> it.groupLink
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
        name : String = group.name,
        frameMap: Map<Node, FfaFrameStructure>? = null,
        unlinkedClusters: List<UnlinkedFrameCluster>? = null) : FfaLayerGroupLinked
    {
        return FfaLayerGroupLinked(this, group, includeSubtrees, name,  frameMap, unlinkedClusters)
                .also { addLayer(it)}
    }

    fun addLexicalLayer(group: GroupNode, name : String = group.name, lexicon: String = "", map: Map<Char, Node>? = null) : FfaLayerLexical
    {
        val existingMap = _layers.asSequence()
                .filterIsInstance<FfaLayerLexical>()
                .filter { it.groupLink == group }
                .firstOrNull()?.sharedExplicitMap
        val mapToUse = existingMap ?: (map?.toMutableMap()) ?: mutableMapOf()
        val layer = FfaLayerLexical(this, group, lexicon, name, mapToUse)
        addLayer(layer)
        return  layer
    }

    fun addCascadingLayer(
        group: GroupNode,
        name : String = group.name,
        infoToImport: List<FfaCascadingSublayerContract>? = null,
        lexicon: String? = null)
            : FfaLayerCascading
    {
        return FfaLayerCascading(this, group, name,infoToImport, lexicon)
                .also { addLayer(it) }
    }

    private fun addLayer( layer: IFfaLayer) {
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


    override fun dupe() = FixedFrameAnimation(
            name + "_dupe",
            workspace,
            layers.map {{context: FixedFrameAnimation -> it.dupe(context)}})
}

object FFALayerCache {

}