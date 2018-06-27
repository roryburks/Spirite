package spirite.base.graphics.gl.stroke

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.GraphicsContext.Composite.DST_OUT
import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod.DEFAULT
import spirite.base.graphics.using
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.IStrokeDrawer
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.ERASE
import spirite.base.util.ceil
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.glu.GLC
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec3
import spirite.base.util.round
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

// TODO: It really shouldn't be hard to make pixel behavior behave as expected.
class GLStrikeDrawerPixel(gle: GLEngine)
    : GLStrokeDrawer(gle)
{
    override fun doStart(context: DrawerContext) {

        println(context.builder.currentPoints.x[0].toString() + "," + context.builder.currentPoints.y[0].toString())
        drawPoint(gle, context.image, context.builder.currentPoints.x[0].floor, context.builder.currentPoints.y[0].floor, context.glParams)
    }

    override fun doStep(context: DrawerContext) {

        drawStroke( gle, context.image, context.builder.currentPoints, context.glParams)
        (0..context.builder.currentPoints.length-1).forEach {
            drawPoint(gle, context.image, context.builder.currentPoints.x[it].floor, context.builder.currentPoints.y[it].floor, context.glParams)
        }
    }

    override fun doBatch(image: GLImage, drawPoints: DrawPoints, params: StrokeParams, glParams: GLParameters) {
        drawStroke( gle, image, drawPoints, glParams)
        (0..drawPoints.length-1).forEach {
            drawPoint(gle, image, drawPoints.x[it].floor, drawPoints.y[it].floor, glParams)
        }
    }

    override fun getIntensifyMethod(params: StrokeParams): IntensifyMethod  = DEFAULT

    companion object {
        fun drawPoint( gle: GLEngine, image: GLImage, x:Int, y:Int, params: GLParameters) {
            gle.setTarget(image)
            val data = floatArrayOf(
                    x+0f, y+0f,
                    x+1f, y+0f,
                    x+0f, y+1f,
                    x+1f, y+1f)
            val prim = GLPrimitive(data, intArrayOf(2), intArrayOf(GLC.TRIANGLE_STRIP), intArrayOf(4))
            gle.applyPrimitiveProgram(PolyRenderCall(Vec3(1f,1f,1f), 1f),
                    prim, params, null)
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