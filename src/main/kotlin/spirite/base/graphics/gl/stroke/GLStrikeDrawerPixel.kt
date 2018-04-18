package spirite.base.graphics.gl.stroke

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.GraphicsContext.Composite.DST_OUT
import spirite.base.graphics.gl.*
import spirite.base.graphics.using
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.IStrokeDrawer
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.ERASE
import spirite.base.util.ceil
import spirite.base.util.f
import spirite.base.util.glu.GLC
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec3
import spirite.base.util.round
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

// TODO: It really shouldn't be hard to make pixel behavior behave as expected.
class GLStrikeDrawerPixel(val gle: GLEngine)
    : IStrokeDrawer
{
    private class DrawerContext(
            val builder: StrokeBuilder,
            val image: GLImage,
            val glParams: GLParameters)
    private var context : DrawerContext? = null

    override fun start(builder: StrokeBuilder, width: Int, height: Int): Boolean {
        val image = GLImage(width, height, gle, false)
        val glParams = GLParameters(width, height, premultiplied = false)

        gle.setTarget(image)
        context = DrawerContext( builder, image, glParams)
        drawPoint( gle, image, builder.currentPoints.x[0].round, builder.currentPoints.y[0].round, glParams)
        return true
    }

    override fun step(): Boolean {
        val ctx = context
        when( ctx) {
            null -> {
                MDebug.handleError(STRUCTURAL, "Tried to continue Stroke that isn't started.")
                return false
            }
            else -> {
                drawStroke( gle, ctx.image, ctx.builder.currentPoints, ctx.glParams)
                return true
            }
        }
    }

    override fun draw(gc: GraphicsContext) {
        context?.apply { drawStrokeImageToGc(image, gc, this.builder.params) }
    }

    override fun end() {
        context?.image?.flush()
        context = null
    }

    override fun batchDraw(gc: GraphicsContext, drawPoints: DrawPoints, params: StrokeParams, width: Int, height: Int) {
        using( GLImage(width, height, gle, false) ) { batchImage ->
            val glParams = batchImage.glParams
            drawStroke( gle, batchImage, drawPoints, glParams)

            drawStrokeImageToGc(batchImage, gc, params)
        }
    }

    private fun drawStrokeImageToGc(image: GLImage, gc: GraphicsContext, strokeParams: StrokeParams) {
        val glgc = gc as? GLGraphicsContext ?: return

        glgc.pushState()

        when( strokeParams.method) {
            ERASE -> {glgc.composite = DST_OUT
            }
        }

        glgc.applyPassProgram(StrokeV2ApplyCall(strokeParams.color.rgbComponent, strokeParams.alpha * gc.alpha), image)

        glgc.popState()
    }

    companion object {
        fun drawPoint( gle: GLEngine, image: GLImage, x:Int, y:Int, params: GLParameters) {
//            val data = floatArrayOf(
//                    x-0.5f, y-0.5f,
//                    x+0.5f, y-0.5f,
//                    x-0.5f, y+0.5f,
//                    x+0.5f, y+0.5f)
//            val prim = GLPrimitive(data, intArrayOf(2), intArrayOf(GLC.TRIANGLE_STRIP), intArrayOf(4))
//            gle.applyPrimitiveProgram(PolyRenderCall(Vec3(1f,1f,1f), 1f),
//                    prim, params, null)
        }

        fun drawStroke( gle: GLEngine, image: GLImage, points: DrawPoints, params: GLParameters) {
            val vb = composeVBuffer(points)

            image.graphics.clear()
            gle.setTarget(image)

            val prim = GLPrimitive(vb, intArrayOf(2), intArrayOf(GLC.LINE_STRIP), intArrayOf(points.length + 1))
            gle.applyPrimitiveProgram(StrokePixelCall(Vec3(1f,1f,1f)),
                    prim, params, null)

            //drawPoint(gle, image, points.x.last().ceil+1, points.y.last().ceil, params)
        }

        private fun composeVBuffer( states: DrawPoints) : FloatArray {
            val STRIDE = 2

            val num = states.length
            val raw = FloatArray(STRIDE * (num + 1))

            for (i in 0 until states.length) {
                // x y z w
                val xy = Vec2(states.x[i], states.y[i])
                raw[STRIDE*i + 0] = xy.x
                raw[STRIDE*i + 1] = xy.y
            }
            raw[STRIDE*num + 0] = states.x.last()
            raw[STRIDE*num + 1] = states.y.last()

            return raw
        }
    }

}