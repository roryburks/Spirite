package spirite.base.imageData.animation.ffa

import rb.extendo.delegates.OnChangeDelegate
import spirite.base.imageData.animation.DefaultAnimCharMap
import spirite.base.imageData.animation.ffa.FfaFrameStructure.Marker.FRAME
import spirite.base.imageData.animation.ffa.FfaFrameStructure.Marker.GAP
import spirite.base.imageData.animation.ffa.FixedFrameAnimation.FFAUpdateContract
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node

class FfaLayerLexical
constructor(
    context: FixedFrameAnimation,
    val groupLink: GroupNode,
    lexicon: String = "",
    name: String = groupLink.name,
    val sharedExplicitMap: MutableMap<Char, Node> = mutableMapOf(),
    asynchronous: Boolean = false)
    : FFALayer(context, asynchronous), IFFALayerLinked
{
    // TODO: Make Undoable?
    override var name by OnChangeDelegate(name) { anim.triggerFFAChange(this)}

    private val lexicalMap : MutableMap<Char, Node> = mutableMapOf()
    var lexicon: String = lexicon
        get() {
            val gapChar = when {
                field.contains(' ') -> ' '
                field.contains('-') -> '-'
                field.contains('_') -> '_'
                else -> ' '
            }

            val sb = StringBuilder()
            for( frame in _frames) {
                if( frame.marker == GAP)
                    repeat(frame.length) {sb.append(gapChar)}
                if( frame.node != null) {
                    val keyCode  = lexicalMap.asSequence()
                            .filter { it.value == frame.node }
                            .firstOrNull()?.key ?: continue
                    repeat(frame.length) {sb.append(keyCode)}
                }
            }
            val newLexicon = sb.toString()
            if( field != newLexicon) {
                field = newLexicon
            }

            return field
        }
        set(value) {
            if( field != value) {
                field = value
                buildLexicon(value)
            }
        }

    init {
        groupLinkUpdated()
        buildLexicon(lexicon)
    }


    // region IFFALayerLinked
    override fun shouldUpdate(contract: FFAUpdateContract): Boolean = contract.changedNodes.contains(groupLink)
    override fun groupLinkUpdated() {
        lexicalMap.clear()

        // Remap as best we can
        val alphabetSansExplicit = DefaultAnimCharMap.getCharList().asSequence()
            .filter { !sharedExplicitMap.containsKey(it) }

        groupLink.children.asReversed().asSequence()
                .filterIsInstance<LayerNode>()
                .zip(alphabetSansExplicit)
                .forEach { lexicalMap[it.second] = it.first }
        sharedExplicitMap.forEach { (t, u) -> lexicalMap[t] = u }

        // Remove any references to no-longer-extant layers
        val remainingNodes = groupLink.children.toHashSet()
        val removedAny = _frames.removeIf { it.node?.run { !remainingNodes.contains(this) } ?: false }
        sharedExplicitMap.values.removeIf { !remainingNodes.contains(it) } // Note: doesn't behave super elegantly with Undo

        if( removedAny)
            anim.triggerFFAChange(this)
    }
    // endregion


    private fun buildLexicon(lexicon: String) {
        _frames.clear()

        lexicon.asSequence()
                .mapNotNull {
                    when( it) {
                        ' ', '_', '-' -> FfaFrameStructure(null, GAP, 1)
                        else -> {
                            val node = lexicalMap[it] ?: return@mapNotNull null
                            FfaFrameStructure(node, FRAME, 1)
                        }
                    }
                }
                .forEach { _frames.add(FFAFrame(it)) }

        anim.triggerFFAChange(this)
    }

    override fun dupe(context: FixedFrameAnimation) = FfaLayerLexical(
            context,
            groupLink,
            lexicon,
            name,
            sharedExplicitMap.toMutableMap(),
            asynchronous)
}