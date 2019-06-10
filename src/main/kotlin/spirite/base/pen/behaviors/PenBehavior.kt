package spirite.base.pen.behaviors

import spirite.base.brains.toolset.IToolsetManager
import spirite.base.imageData.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.Penner
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method
import sgui.generic.color.Color


object PenBehavior {
    class Stroke(penner: Penner, drawer: IStrokeModule, val color: Color) : StrokeBehavior(penner,drawer) {
        override fun makeStroke() = PenBehavior.makeStroke(penner.toolsetManager, color)
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