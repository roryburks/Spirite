package rb.animo

import rb.animo.animation.RenderProperties
import rb.glow.IGraphicsContext

data class DrawContract(
        val depth: Int,
        val renderProperties: RenderProperties? = null,
        val drawRubrick : (IGraphicsContext)->Unit)