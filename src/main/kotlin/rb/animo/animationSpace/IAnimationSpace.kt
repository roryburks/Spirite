package rb.animo.animationSpace

import rb.animo.animation.IAnimation

interface IAnimationSpace {
    fun buildAnimation(lexicon: String) : IAnimation
}