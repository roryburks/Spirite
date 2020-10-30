package spirite.base.pen.stroke

import rb.glow.Color
import rb.glow.Colors
import spirite.base.brains.toolset.PenDrawMode
import spirite.base.brains.toolset.PenDrawMode.NORMAL
import spirite.base.pen.stroke.StrokeParams.Method.BASIC

data class StrokeParams(
        val color: Color = Colors.BLACK,
        val method: Method = BASIC,
        val mode: PenDrawMode = NORMAL,
        val width: Float = 1.0f,
        val alpha: Float = 1.0f,
        val dynamics: PenDynamics = BasicDynamics,
        val interpolationMethod: InterpolationMethod = InterpolationMethod.CUBIC_SPLINE,
        val hard : Boolean = false,
        val maxWidth : Float = 25f)
{
    enum class Method constructor(val fileId: Int) {
        BASIC(0),
        ERASE(1),
        PIXEL(2)
        ;

        companion object {
            fun fromFileId(fid: Int): Method? = Method.values().find { it.fileId == fid }
        }
    }

    enum class InterpolationMethod {
        NONE,
        CUBIC_SPLINE
    }
}

