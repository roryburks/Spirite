package spirite.base.pen.behaviors

import rb.glow.color.Color
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.brains.toolset.PenDrawMode.NORMAL
import spirite.base.imageData.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.Penner
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.ERASE


object EraserBehavior {
    class Stroke(penner: Penner, drawer: IStrokeModule, val color: Color) : StrokeBehavior(penner,drawer) {
        override fun makeStroke() = EraserBehavior.makeStroke(penner.toolsetManager, color)
    }

    private fun makeStroke(toolsetManager: IToolsetManager, color: Color) : StrokeParams {
        val settings = toolsetManager.toolset.Eraser

        return StrokeParams(
                color,
                ERASE,
                NORMAL,
                settings.width,
                settings.alpha,
                hard = settings.hard)
    }
}