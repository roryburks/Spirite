package spirite.base.imageData.animationSpaces.FFASpace

import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.groupExtensions.then

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
    var pos: Int? = null

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

    var falloverPoint = 0
    var nextBreakpointAfter = 0
    var moveToPoint = 0
    lateinit var moveToAnim :FixedFrameAnimation
    var caret = 0

    override fun advance(miliseconds: Float){

        if( pos == null) {
            pos = 0
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
            val nextMet = MathUtil.cycle(anim.start,anim.end, nextF.floor)

            if( nextMet != currentMet) {
                if( currentMet == nextBreakpointAfter) {

                }
            }
        }
    }

    private fun determineNextBreakpoint()
    {
        falloverPoint = state.met.floor

        val char1 = lexicon[caret]
        val char2 = lexicon.getOrNull(caret+1)
        caret += 1
        if( char2 == null) {
            nextBreakpointAfter = state.animation?.end ?: 0
        }

        val anim1 = state.charbinds.entries.firstOrNull{it.value == char1}?.key ?: return
        val anim2 = state.charbinds.entries.firstOrNull{it.value == char2}?.key ?: return

        if( anim1 == anim2) {
            nextBreakpointAfter = anim1.end
            moveToPoint = 0
            moveToAnim = anim1
            return
        }

        val endLink = space.animationStructs.firstOrNull { it.animation == anim1 } ?: return
        if( endLink.onEndLink?.first == anim2) {
            nextBreakpointAfter = anim1.end
            moveToPoint = endLink.onEndLink?.second ?: 0
            moveToAnim = anim2
            return
        }
        else {
            val relevantLink = space.links.firstOrNull { it.origin == anim1 && it.destination == anim2 } ?: return
            nextBreakpointAfter = relevantLink.originFrame
            moveToPoint = relevantLink.destinationFrame
            moveToAnim = relevantLink.destination
            return
        }
    }
}