package spirite.base.pen.stroke

import rb.glow.GraphicsContext
import rb.vectrix.linear.ITransformF

interface IStrokeDrawer {
    fun start( builder: StrokeBuilder, width: Int, height:Int) : Boolean
    fun step() : Boolean
    fun draw( gc: GraphicsContext)
    fun end()

    fun batchDraw(
            gc: GraphicsContext,
            drawPoints: DrawPoints,
            params: StrokeParams,
            width: Int,
            height: Int)
}