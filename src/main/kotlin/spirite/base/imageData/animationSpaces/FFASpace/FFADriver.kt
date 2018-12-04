package spirite.base.imageData.animationSpaces.FFASpace

import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import rb.extendo.extensions.then

interface IFFAPlayback {
    fun advance( miliseconds: Float)
}

class FFANormalPlayback(
        val space: FFAAnimationSpace) : IFFAPlayback
{
    val state get() = space.stateView
    override fun advance(miliseconds: Float) {
        state.run {
            val anim = animation ?: return
            val nextMet = met + fps / (1000.0f / miliseconds)
            if (nextMet >= anim.end) {
                val link = space.animationStructs.firstOrNull { it.animation == anim }?.onEndLink
                if (link != null) {
                    animation = link.first
                    met = link.second + (nextMet - nextMet.floor)
                } else
                    met = MathUtil.cycle(anim.start.f, anim.end.f, nextMet)
            } else {
                met = MathUtil.cycle(anim.start.f, anim.end.f, nextMet)
            }
        }
    }

}

class FFALexicalPlayback(
        val lexicon: String,
        val space: FFAAnimationSpace) : IFFAPlayback
{
    val state get() = space.stateView

    fun validate() : String?
    {
        val charbind = state.charbinds

        // 1: Make sure all chars exist in State Space
        val missisngChars = lexicon.map { it }
                .distinct()
                .filter {!charbind.values.contains(it)}

        if( missisngChars.any())
            return "Characters with no Animations attached: ${missisngChars.joinToString(" ")}"

        // 2: Make sure all the transition contain proper links
        val links = space.links.map { Pair(charbind[it.origin], charbind[it.destination]) }
                .then(space.animationStructs.mapNotNull { it.onEndLink?.run { Pair(charbind[it.animation], charbind[first]) } } )
                .toList()
                .mapNotNull { if( it.first == null || it.second == null)null else Pair<Char,Char>(it.first!!,it.second!!) }
                .distinct()
                .toHashSet()

        val invalidLinks = (0 until lexicon.length-1).mapNotNull {index ->
            val link = Pair(lexicon[index],lexicon[index+1])

            when {
                link.first == link.second -> null
                links.contains(link) -> null
                else ->link
            }
        }
        if( invalidLinks.any())
            return "Invalid Links: ${invalidLinks.distinct().joinToString(", ") { "${it.first} -> ${it.second}" }}"

        return null
    }

    var caret: Int? = null
    var falloverPoint = 0
    var nextBreakpointAfter = 0
    var moveToFrame = 0
    var moveToAnim :FixedFrameAnimation? = null

    override fun advance(miliseconds: Float){

        if( caret == null) {
            caret = 0
            val char = lexicon.getOrNull(0) ?: return
            val animation= state.charbinds.entries.firstOrNull { it.value == char }?.key ?: return
            state.animation = animation
            state.met = animation.start.f
            determineNextBreakpoint()
        }
        else {
            val anim = state.animation ?: return
            val nextF = state.met + state.fps / (1000.0f / miliseconds)
            val currentMet = state.met.floor
            val nextMet = nextF.floor

            if( nextMet != currentMet && currentMet == nextBreakpointAfter) {
                if( moveToAnim == null) {
                    caret = null
                }
                else {
                    state.animation = moveToAnim
                    state.met = nextF - nextMet + moveToFrame
                    determineNextBreakpoint()
                }
            }
            else {
                state.met = MathUtil.cycle(anim.start.f, anim.end.f, nextF)
            }
        }
    }

    private fun determineNextBreakpoint()
    {
        falloverPoint = state.met.floor

        val ccaret = caret ?: return
        val char1 = lexicon[ccaret]
        val char2 = lexicon.getOrNull(ccaret+1)
        caret = ccaret + 1
        if( char2 == null) {
            nextBreakpointAfter = state.animation?.run{end-1} ?: 0
            moveToAnim = null
        }

        val anim1 = state.charbinds.entries.firstOrNull{it.value == char1}?.key ?: return
        val anim2 = state.charbinds.entries.firstOrNull{it.value == char2}?.key ?: return

        if( anim1 == anim2) {
            nextBreakpointAfter = anim1.end-1
            moveToFrame = 0
            moveToAnim = anim1
            return
        }

        val endLink = space.animationStructs.firstOrNull { it.animation == anim1 } ?: return
        if( endLink.onEndLink?.first == anim2) {
            nextBreakpointAfter = anim1.end-1
            moveToFrame = endLink.onEndLink?.second ?: 0
            moveToAnim = anim2
            return
        }
        else {
            val relevantLink = space.links.firstOrNull { it.origin == anim1 && it.destination == anim2 } ?: return
            nextBreakpointAfter = relevantLink.originFrame
            moveToFrame = relevantLink.destinationFrame
            moveToAnim = relevantLink.destination
            return
        }
    }
}