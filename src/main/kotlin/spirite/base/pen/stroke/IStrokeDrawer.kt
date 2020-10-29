package spirite.base.pen.stroke

import rb.glow.IGraphicsContext

interface IStrokeDrawer {
    fun start( builder: StrokeBuilder, width: Int, height:Int) : Boolean
    fun step() : Boolean
    fun draw( gc: IGraphicsContext)
    fun end()

    fun batchDraw(
            gc: IGraphicsContext,
            drawPoints: DrawPoints,
            params: StrokeParams,
            width: Int,
            height: Int)
}