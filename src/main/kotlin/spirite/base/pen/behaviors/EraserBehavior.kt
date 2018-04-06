package spirite.base.pen.behaviors

import spirite.base.brains.toolset.IToolsetManager
import spirite.base.imageData.mediums.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.Penner
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.BASIC
import spirite.base.pen.stroke.StrokeParams.Method.ERASE
import spirite.base.util.Color


object EraserBehavior {
    class Stroke(penner: Penner, drawer: IStrokeModule, val color: Color) : StrokeBehavior(penner,drawer) {
        override fun makeStroke() = _makeStroke(penner.toolsetManager, color)
    }


    private fun _makeStroke(toolsetManager: IToolsetManager, color: Color) : StrokeParams {
        val settings = toolsetManager.toolset.Pen

        return StrokeParams(
                color,
                ERASE,
                settings.mode,
                settings.width,
                settings.alpha,
                hard = settings.hard)
    }
}