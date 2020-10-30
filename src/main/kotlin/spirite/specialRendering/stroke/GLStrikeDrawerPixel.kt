package spirite.specialRendering.stroke

import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.gl.shader.programs.PolyRenderCall
import rb.glow.gle.GLParameters
import rb.glow.gle.GLPrimitive
import rb.glow.gle.IGLEngine
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.Vec2f
import rb.vectrix.linear.Vec3f
import rb.vectrix.mathUtil.floor
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
import spirite.specialRendering.StrokePixelCall
import spirite.specialRendering.StrokeV2ApplyCall.IntensifyMethod
import spirite.specialRendering.StrokeV2ApplyCall.IntensifyMethod.DEFAULT

class GLStrikeDrawerPixel(gle: IGLEngine)
    : GLStrokeDrawer(gle)
{
    override fun doStart(context: DrawerContext) {
        drawPoint(gle, context.image, context.builder.currentPoints.x[0].floor, context.builder.currentPoints.y[0].floor, context.glParams)
    }

    override fun doStep(context: DrawerContext) {
        drawStroke(gle, context.image, context.builder.currentPoints, context.glParams, null)
        (0 until context.builder.currentPoints.length).forEach {
            drawPoint(gle, context.image, context.builder.currentPoints.x[it].floor, context.builder.currentPoints.y[it].floor, context.glParams)
        }
    }

    override fun doBatch(image: GLImage, drawPoints: DrawPoints, params: StrokeParams, glParams: GLParameters, transform: ITransform?) {
        drawStroke(gle, image, drawPoints, glParams, transform)
        (0 until drawPoints.length).forEach {
            val px: Int
            val py: Int
            if( transform == null) {
                px = drawPoints.x[it].floor
                py = drawPoints.y[it].floor
            }
            else {
                val point = transform.apply(Vec2f(drawPoints.x[it], drawPoints.y[it]))
                px = point.x.floor
                py = point.y.floor
            }
            drawPoint(gle, image, px, py, glParams)
        }
    }

    override fun getIntensifyMethod(params: StrokeParams): IntensifyMethod  = DEFAULT

    companion object {
        fun drawPoint(gle: IGLEngine, image: GLImage, x:Int, y:Int, params: GLParameters) {
            gle.setTarget(image)
            val data = floatArrayOf(
                    x+0f, y+0f,
                    x+1f, y+0f,
                    x+0f, y+1f,
                    x+1f, y+1f)
            val prim = GLPrimitive(data, intArrayOf(2), intArrayOf(GLC.TRIANGLE_STRIP), intArrayOf(4))
            gle.applyPrimitiveProgram(PolyRenderCall(Vec3f(1f, 1f, 1f), 1f),
                    prim, params, null)
        }

        fun drawStroke(gle: IGLEngine, image: GLImage, points: DrawPoints, params: GLParameters, transform: ITransform?) {
            val vb = composeVBuffer(points)

            image.graphics.clear()
            gle.setTarget(image)

            val prim = GLPrimitive(vb, intArrayOf(2), intArrayOf(GLC.LINE_STRIP), intArrayOf(points.length + 1))
            gle.applyPrimitiveProgram(StrokePixelCall(Vec3f(1f, 1f, 1f)),
                    prim, params, transform)
        }

        private fun composeVBuffer( states: DrawPoints) : FloatArray {
            val STRIDE = 2

            val num = states.length
            val raw = FloatArray(STRIDE * (num + 1))

            for (i in 0 until states.length) {
                // xi yi zf wf
                val xy = Vec2f(states.x[i], states.y[i])
                raw[STRIDE*i + 0] = xy.xf
                raw[STRIDE*i + 1] = xy.yf
            }
            raw[STRIDE*num + 0] = states.x.last()
            raw[STRIDE*num + 1] = states.y.last()

            return raw
        }
    }

}