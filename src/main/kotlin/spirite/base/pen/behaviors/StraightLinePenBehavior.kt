package spirite.base.pen.behaviors

import rb.vectrix.mathUtil.f
import rb.glow.SColor
import spirite.base.brains.toolset.IToolsetManager
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.pen.PenState
import spirite.base.pen.Penner
import spirite.base.pen.stroke.StrokeParams
import kotlin.math.max

class StraightLinePenBehavior (
        penner: Penner,
        val drawer: IImageDrawer.IStrokeModule,
        val params: StrokeParams) : PennerBehavior(penner)
{
    var sx: Int = 0
    var sy: Int = 0
    var maxP : Float = 0f

    override fun onStart() {
        sx = penner.x
        sy = penner.y
        maxP = penner.pressure
    }

    override fun onTock() {
        maxP = max(maxP,  penner.pressure)
    }

    override fun onMove() { }

    override fun onEnd() {
        drawer.startStroke(params, PenState(sx.f, sy.f, maxP))
        drawer.stepStroke(PenState(penner.xf, penner.yf, maxP))
        drawer.endStroke()
    }

    companion object{
        fun FromSettings(penner: Penner, drawer: IImageDrawer.IStrokeModule, toolsetManager:IToolsetManager, color: SColor) : StraightLinePenBehavior
        {
            val settings = toolsetManager.toolset.Pen

            val sp = StrokeParams(
                    color,
                    StrokeParams.Method.BASIC,
                    settings.mode,
                    settings.width,
                    settings.alpha,
                    hard = settings.hard)

            return StraightLinePenBehavior(penner, drawer, sp)
        }
    }
}