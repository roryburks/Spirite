package spirite.base.pen.stroke

import spirite.base.graphics.IImage

interface IStrokeDrawer {
    /** The List passed should be a list which updates as the history gets constructed, not a baked snapshot.  */
    fun start( builder: StrokeBuilder, width: Int, height:Int) : Boolean
    fun step() : Boolean
    fun end()
    fun batchDraw(drawPoints: DrawPoints, params: StrokeParams, width: Int, height: Int) : IImage
}
//
//abstract class BatchStrokeDrawer : IStrokeDrawer {
//    private class BatchStrokeContext(
//            val builder: StrokeBuilder,
//            val image : RawImage)
//
//
//    private var activeContext : BatchStrokeContext? = null
//
//    override fun start( builder: StrokeBuilder, width: Int, height:Int) : Boolean{
//        val image = Hybrid.imageCreator.createImage(width, height)
//        activeContext = BatchStrokeContext(
//                builder,
//                image)
//        return batchDraw(image.graphics, builder.currentPoints, builder.params)
//    }
//
//    override fun step() : Boolean{
//        val context = this.activeContext
//        when( context) {
//            null -> {
//                MDebug.handleError(STRUCTURAL, "Tried to step in a stroke that isn't started.")
//                return false
//            }
//            else -> return batchDraw(context.image.graphics, context.builder.currentPoints, context.builder.params)
//        }
//    }
//
//    override fun end() {
//        activeContext?.image?.flush()
//        activeContext = null
//    }
//
//
//    abstract override fun batchDraw(gc: GraphicsContext, drawPoints: DrawPoints, params: StrokeParams) : Boolean
//}