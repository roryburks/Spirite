package spirite.base.pen.stroke

import spirite.base.graphics.GraphicsContext

interface IStrokeDrawer {
    /** The List passed should be a list which updates as the history gets constructed, not a baked snapshot.  */
    fun start( builder: StrokeBuilder, width: Int, height:Int) : Boolean
    fun step() : Boolean
    fun draw( gc: GraphicsContext)
    fun end()

    fun batchDraw(gc: GraphicsContext, drawPoints: DrawPoints, params: StrokeParams, width: Int, height: Int)
}