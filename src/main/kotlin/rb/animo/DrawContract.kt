package rb.animo

import rb.animo.animation.AnimationDrawSettings
import rb.glow.IGraphicsContext

data class DrawContract(
    val depth: Int,
    val drawSettings: AnimationDrawSettings? = null,
    val drawRubrick : (IGraphicsContext)->Unit)