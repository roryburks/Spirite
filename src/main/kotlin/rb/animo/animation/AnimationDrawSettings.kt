package rb.animo.animation

import rb.glow.Composite
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.ImmutableTransformD

data class AnimationDrawSettings(
    val trans: ITransform = ImmutableTransformD.Identity,
    val alpha: Float = 1f,
    val composite: Composite = Composite.SRC_OVER,
    val groupMap : Map<Char, AnimationGroupSettings> = mapOf() )
{
    companion object{
        val Basic = AnimationDrawSettings()
    }
}

data class AnimationGroupSettings(
    val drawn : Boolean = false,
    val alpha: Float = 1f)