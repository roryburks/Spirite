package spirite.base.pen.behaviors

import spirite.base.brains.toolset.IToolsetManager
import spirite.base.imageData.mediums.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.Penner
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.InterpolationMethod.NONE
import spirite.base.pen.stroke.StrokeParams.Method.BASIC
import spirite.base.pen.stroke.StrokeParams.Method.PIXEL
import spirite.base.util.Color


object PixelBehavior {
    class Stroke(penner: Penner, drawer: IStrokeModule, val color: Color) : StrokeBehavior(penner,drawer) {
        override fun makeStroke() = _makeStroke(penner.toolsetManager, color)
    }


    private fun _makeStroke(toolsetManager: IToolsetManager, color: Color) : StrokeParams {
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