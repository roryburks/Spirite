package spirite.base.imageData.animation.ffa

import rb.extendo.extensions.toHashMap
import rb.vectrix.mathUtil.MathUtil
import spirite.base.imageData.animation.ffa.FixedFrameAnimation.FFAUpdateContract
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import kotlin.math.min

class FfaLayerCascading(
        override val anim: FixedFrameAnimation,
        val groupLink: GroupNode,
        lexicon: String = "")
    :IFfaLayer, IFFALayerLinked
{
    var lexicon: String? = null
        set(value) {
            if( field != value) {
                field = value
                update()
            }
        }

    var sublayerInfo = mutableMapOf<GroupNode, FFACascadingSublayerInfo>()
    //var sublayers : List<Pair<Int, FFACascadingSublayerInfo?>> = emptyList()

    // region IFfaLayer
    override val start: Int get() = 0
    override var end: Int = 0 ; private set
    override var asynchronous: Boolean = false
    override var frames: List<IFFAFrame> = emptyList(); private set
    override fun getFrameFromLocalMet(met: Int, loop: Boolean): IFFAFrame? =
            frames.getOrNull(if( loop) MathUtil.cycle(0, end, met) else met)
    // endregion

    // region IFFALayerLinked
    override fun groupLinkUpdated() {update()}
    override fun shouldUpdate(contract: FFAUpdateContract) = contract.ancestors.contains(groupLink)
    // Endregion

    // region exposed functionality
    // endregion

    private data class IndexedSublayer(val start: Int, val info: FFACascadingSublayerInfo?)

    private fun update()
    {
        val newGroupNodes = groupLink.children.filterIsInstance<GroupNode>().asReversed()

        val oldSublayerInfo = sublayerInfo
        val newSublayerInfo = newGroupNodes
                .mapIndexedNotNull { index, groupNode ->
                    val key = 'A' + index
                    val info = oldSublayerInfo[groupNode]?.copyUpdated(key)
                            ?: FFACascadingSublayerInfo.FromGroup(groupNode, key)
                            ?: return@mapIndexedNotNull  null
                    Pair(groupNode, info)
                }
                .toMap()
        sublayerInfo = newSublayerInfo.toMutableMap()

        val oldLexicalMap = oldSublayerInfo.values.toHashMap({it.lexicalKey}, {it.group})
        val newLexicalMap = newSublayerInfo.values.toHashMap ({it.lexicalKey}, {it})
        val newLexicalInvMap = newSublayerInfo.values.toHashMap({it.group}, {it.lexicalKey})

        // Remap lexicon
        val oldLexicon = lexicon
        lexicon = oldLexicon?.asSequence()
                ?.mapNotNull { oldLexicalMap[it]?.run { newLexicalInvMap[this] } }
                ?.joinToString()
        val newLexicon = lexicon ?: newLexicalInvMap.values.joinToString()

        var met = 0
        val sublayers = newLexicon.mapNotNull {
            val info = newLexicalMap[it]
            IndexedSublayer(met, info).also { met += info?.primaryLen ?: 1 }
        }

        // Recalc Frames
        calcFrames(sublayers)

        anim.triggerFFAChange(this)
    }

    private fun calcFrames( sublayers: List<IndexedSublayer>)
    {
        // Recalc Len
        end = sublayers.asSequence().map { it.start + (it.info?.len ?: 1) }.max() ?: 0

        frames = (0 until end).map { i -> CascadingFrame(
                i,
                sublayers
                        .filter {it.info != null && it.start <= i && (it.start + it.info.primaryLen ) > i }
                        .map { Pair(i - it.start, it.info!!) }
                )
        }
    }


    inner class CascadingFrame(
            override val start: Int,
            val points: List<Pair<Int, FFACascadingSublayerInfo>>)
        :IFFAFrame
    {
        override val layer: IFfaLayer get() = this@FfaLayerCascading
        override val length: Int get() = 1
        override fun getDrawList() = points
                .mapNotNull { it.second.getLayerFromLocalMet(it.first) }
                .flatMap { it.getDrawList() }
    }
}


data class FFACascadingSublayerInfo (
        val group: GroupNode,
        val lexicalKey: Char,
        val layers: List<LayerNode>,
        val primaryLen: Int,
        val lexicon: String? = null )
{
    val len: Int get() = lexicon?.length ?: layers.count()

    fun copyUpdated(newLexicalKey: Char) : FFACascadingSublayerInfo? {
        val newLayerNodes = group.children.filterIsInstance<LayerNode>()
                .asReversed()
        if(!newLayerNodes.any())
            return null
        val plen = min(primaryLen, newLayerNodes.size)

        // TODO: Remap Lexicon

        return this.copy(layers = newLayerNodes, lexicalKey = newLexicalKey, primaryLen =  plen)
    }

    fun getLayerFromLocalMet(met: Int) : LayerNode? = when {
        met < 0 -> null
        lexicon == null -> layers.getOrNull(met)
        else -> when(val lex = lexicon.getOrNull(met)) {
            null -> null
            else -> layers.getOrNull(lex - 'A')
        }
    }

    companion object {
        fun FromGroup(group: GroupNode, lexicalKey: Char) : FFACascadingSublayerInfo? {
            val layerNodes = group.children.filterIsInstance<LayerNode>()
                    .asReversed()
            if( !layerNodes.any())
                return null
            return FFACascadingSublayerInfo(
                    group,
                    lexicalKey,
                    layerNodes,
                    layerNodes.count())
        }
    }
}