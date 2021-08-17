package spirite.base.pen.behaviors

import rb.glow.Color
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.graphics.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.Penner
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.InterpolationMethod.NONE
import spirite.base.pen.stroke.StrokeParams.Method.PIXEL


object PixelBehavior {
    class Stroke(penner: Penner, drawer: IStrokeModule, val color: Color) : StrokeBehavior(penner,drawer) {
        override fun makeStroke() = PixelBehavior.makeStroke(penner.toolsetManager, color)
    }


    private fun makeStroke(toolsetManager: IToolsetManager, color: Color) : StrokeParams {
        val settings = toolsetManager.toolset.Pen

        return StrokeParams(
                color,
                PIXEL,
                settings.mode,
                1f,
                settings.alpha,
                interpolationMethod = NONE)
    }
}