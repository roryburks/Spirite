package spirite.base.imageData.animationSpaces.FFASpace

import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.base.util.floor

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

    override fun advance(miliseconds: Float){
        if( pos == null) {
            pos = 0
            val char = lexicon.getOrNull(0) ?: return
            val animation= state.charbinds.entries.firstOrNull { it.value == char }?.key ?: return
            state.animation = animation
            state.met = animation.start.f
        }
    }
}