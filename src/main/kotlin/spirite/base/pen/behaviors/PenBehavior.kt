package spirite.base.pen.behaviors

import rb.glow.Color
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.imageData.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.Penner
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method


object PenBehavior {
    class Stroke(penner: Penner, drawer: IStrokeModule, val color: Color) : StrokeBehavior(penner,drawer) {
        override fun makeStroke() = makeStroke(penner.toolsetManager, color)
    }

    private fun makeStroke(toolsetManager: IToolsetManager, color: Color) : StrokeParams{
        val settings = toolsetManager.toolset.Pen

        return StrokeParams(
                color,
                Method.BASIC,
                settings.mode,
                settings.width,
                settings.alpha,
                hard = settings.hard)
    }
}