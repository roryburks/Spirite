package spirite.base.pen.behaviors

import spirite.base.imageData.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.PenState
import spirite.base.pen.Penner
import spirite.base.pen.stroke.StrokeParams
import spirite.hybrid.Hybrid
import kotlin.math.abs

abstract class StrokeBehavior(penner: Penner, val drawer : IStrokeModule) : PennerBehavior(penner)
{
    private var shiftX = penner.rawX
    private var shiftY = penner.rawY
    private var dx = penner.xf
    private var dy = penner.yf
    private var shiftMode = -1  // 0 : Accept Any, 1: Horizontal, 2: Vertical, -1: not holding shift

    override fun onStart() {
        val stroke = makeStroke()

        if( !drawer.canDoStroke(stroke.method) || !drawer.startStroke(stroke, PenState( penner.xf, penner.yf, penner.pressure)) )
        {
            end()
            Hybrid.beep()
        }
    }

    override fun onTock() {
        if( penner.holdingShift) {
            when( shiftMode) {
                -1 -> {
                    shiftMode = 0
                    shiftX = penner.rawX
                    shiftY = penner.rawY
                }
                0 -> {
                    if( abs(shiftX - penner.rawX) > 10) shiftMode = 1
                    else if( abs( shiftY - penner.rawY) > 10) shiftMode = 2
                }
                1 -> dx = penner.xf
                2 -> dy = penner.yf
            }
        }
        else {
            shiftMode = -1
            dx = penner.xf
            dy = penner.yf
        }
        drawer.stepStroke(PenState(dx, dy, penner.pressure))
    }

    override fun onEnd() {drawer.endStroke()}
    override fun onMove() {}

    abstract fun makeStroke(): StrokeParams
}