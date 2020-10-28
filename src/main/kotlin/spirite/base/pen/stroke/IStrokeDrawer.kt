package spirite.base.pen.stroke

import rb.glow.GraphicsContext_old

interface IStrokeDrawer {
    fun start( builder: StrokeBuilder, width: Int, height:Int) : Boolean
    fun step() : Boolean
    fun draw( gc: GraphicsContext_old)
    fun end()

    fun batchDraw(
            gc: GraphicsContext_old,
            drawPoints: DrawPoints,
            params: StrokeParams,
            width: Int,
            height: Int)
}