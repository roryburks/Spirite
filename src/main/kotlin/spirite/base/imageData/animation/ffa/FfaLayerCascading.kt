package spirite.base.imageData.animation.ffa

import rb.extendo.delegates.OnChangeDelegate
import rb.extendo.extensions.toHashMap
import rb.vectrix.mathUtil.MathUtil
import spirite.base.imageData.animation.DefaultAnimCharMap
import spirite.base.imageData.animation.ffa.FixedFrameAnimation.FFAUpdateContract
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import kotlin.math.min

class FfaLayerCascading(
    override val anim: FixedFrameAnimation,
    val groupLink: GroupNode,
    name: String = groupLink.name,
    infoToImport: List<FfaCascadingSublayerContract>? = null,
    lexicon: String? = null,
    asynchronous: Boolean = false)
    :IFfaLayer, IFFALayerLinked
{
    // TODO: Make undoable?
    override var name by OnChangeDelegate(name) { anim.triggerFFAChange(this)}

    var lexicon by OnChangeDelegate<String?>(lexicon) {update()}

    var sublayerInfo = mutableMapOf<GroupNode, FfaCascadingSublayerInfo>()

    // region IFfaLayer
    override val start: Int get() = 0
    override var end: Int = 0 ; private set
    override var asynchronous: Boolean = asynchronous
    override var frames: List<IFfaFrame> = emptyList(); private set
    override fun getFrameFromLocalMet(met: Int, loop: Boolean): IFfaFrame? =
            frames.getOrNull(if( loop) MathUtil.cycle(0, end, met) else met)
    // endregion

    // region IFFALayerLinked
    override fun groupLinkUpdated() {update()}
    override fun shouldUpdate(contract: FFAUpdateContract) = contract.ancestors.contains(groupLink)
    // Endregion


    init {
        infoToImport
                ?.mapNotNull { FfaCascadingSublayerInfo.FromGroup(it.group, it.lexicalKey, it.primaryLen) }
                ?.forEach { sublayerInfo[it.group] =  it }
        update()
    }

    // region exposed functionality
    fun getAllSublayers() = sublayerInfo.entries.map { FfaCascadingSublayerContract(it.value.group, it.value.lexicalKey, it.value.primaryLen, it.value.lexicon) }
    fun getSublayerInfo(node: GroupNode) = sublayerInfo[node]?.run { FfaCascadingSublayerContract(group, lexicalKey, primaryLen, lexicon) }
    fun setSublayerInfo(info: FfaCascadingSublayerContract) {
        if( sublayerInfo.entries.asSequence()
                        .filter { it.key != info.group }
                        .any { it.value.lexicalKey == info.lexicalKey })
        {
            println("Issue in setSublaterInfo")
            //Hybrid.beep()
            // TODO: Better Debug
        }

        val mapped = FfaCascadingSublayerInfo.FromGroup(
                info.group, info.lexicalKey, info.primaryLen, info.lexicon)
        if( mapped != null) {
            sublayerInfo[info.group] = mapped
            anim.triggerFFAChange(this)
        }
        update()
    }
    // endregion

    // region Internal
    private data class IndexedSublayer(val start: Int, val info: FfaCascadingSublayerInfo?)

    private fun update()
    {
        val newGroupNodes = groupLink.children.filterIsInstance<GroupNode>().asReversed()

        val oldSublayerInfo = sublayerInfo
        val newSublayerInfo = newGroupNodes
                .mapIndexedNotNull { index, groupNode ->
                    val key = DefaultAnimCharMap.getCharForIndex(index) ?: ' '
                    val info = oldSublayerInfo[groupNode]?.copyUpdated(key)
                            ?: FfaCascadingSublayerInfo.FromGroup(groupNode, key)
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
                ?.mapNotNull { if(it == ' ') ' ' else oldLexicalMap[it]?.run { newLexicalInvMap[this] } }
                ?.joinToString("")
        val newLexicon = lexicon ?: String(CharArray(newGroupNodes.size) {DefaultAnimCharMap.getCharForIndex(it)?:' '})

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
                        .filter {it.info != null && it.start <= i && (it.start + it.info.len ) > i }
                        .map { Pair(i - it.start, it.info!!) }
                )
        }
    }


    inner class CascadingFrame(
            override val start: Int,
            val points: List<Pair<Int, FfaCascadingSublayerInfo>>)
        :IFfaFrame
    {
        override val layer: IFfaLayer get() = this@FfaLayerCascading
        override val length: Int get() = 1
        override fun getDrawList() = points
                .mapNotNull { it.second.getLayerFromLocalMet(it.first) }
                .flatMap { it.getDrawList() }
    }
    // endregion

    override fun dupe(context: FixedFrameAnimation) = FfaLayerCascading(
            context,
            groupLink,
            name,
            sublayerInfo.map { (_, value) -> FfaCascadingSublayerContract(
                    value.group,
                    value.lexicalKey,
                    value.primaryLen,
                    value.lexicon) },
            lexicon,
            asynchronous)
}

data class FfaCascadingSublayerContract(
    val group: GroupNode,
    val lexicalKey: Char,
    val primaryLen: Int,
    val lexicon: String?)

data class FfaCascadingSublayerInfo
constructor(
    val group: GroupNode,
    val lexicalKey: Char,
    val layers: List<LayerNode>,
    private val _primaryLen: Int? = null,
    val lexicon: String? = null )
{
    val primaryLen: Int get() = _primaryLen ?: len
    val len: Int get() = lexicon?.length ?: layers.count()

    fun copyUpdated(newLexicalKey: Char) : FfaCascadingSublayerInfo? {
        val newLayerNodes = group.children.filterIsInstance<LayerNode>()
                .asReversed()
        if(!newLayerNodes.any())
            return null
        val plen =
                if( _primaryLen == null) null
                else min(_primaryLen, newLayerNodes.size)

        // TODO: Remap Lexicon

        return this.copy(layers = newLayerNodes, lexicalKey = newLexicalKey, _primaryLen =  plen)
    }

    fun getLayerFromLocalMet(met: Int) : LayerNode? = when {
        met < 0 -> null
        lexicon == null -> layers.getOrNull(met)
        else -> when(val lex = lexicon.getOrNull(met)) {
            null -> null
            else -> layers.getOrNull(DefaultAnimCharMap.getIndexFromChar(lex) ?: -1)
        }
    }

    companion object {
        fun FromGroup(
            group: GroupNode,
            lexicalKey: Char,
            primaryLen: Int? = null,
            lexicon: String? = null)
                : FfaCascadingSublayerInfo?
        {
            val layerNodes = group.children.filterIsInstance<LayerNode>()
                    .asReversed()
            if( !layerNodes.any())
                return null
            return FfaCascadingSublayerInfo(
                    group,
                    lexicalKey,
                    layerNodes,
                    primaryLen,
                    lexicon)
        }
    }
}