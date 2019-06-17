package spirite.base.graphics.gl.stroke

import com.hackoeur.jglm.support.FastMath
import rb.glow.gl.GLC
import rb.glow.gl.GLImage
import rb.glow.gle.GLParameters
import rb.glow.gle.GLPrimitive
import rb.glow.gle.IGLEngine
import rb.glow.gle.PolyRenderCall
import rb.glow.glu.PrimitiveBuilder
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.Vec3f
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.f
import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod.DEFAULT
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod.HARD_EDGED
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
import kotlin.math.PI

// Dot
class GLStrokeDrawerV3(
        gle: IGLEngine) : GLStrokeDrawer(gle)
{
    override fun doStart(context: DrawerContext) {
        drawStroke(context.image, context.builder.currentPoints, context.builder.params.width, context.glParams, context.builder.params)
    }

    override fun doStep(context: DrawerContext) {
        drawStroke(context.image, context.builder.currentPoints, context.builder.params.width, context.glParams, context.builder.params)
    }

    override fun doBatch(image: GLImage, drawPoints: DrawPoints, params: StrokeParams, glParams: GLParameters) {
        drawStroke( image, drawPoints, params.width, glParams, params)
    }

    override fun getIntensifyMethod(params: StrokeParams): IntensifyMethod  = when {
        params.hard -> HARD_EDGED
        else -> DEFAULT
    }

    private fun drawStroke(target: GLImage, states: DrawPoints, lineWidth: Float, params: GLParameters, strokeParams: StrokeParams, trans: ITransformF? = null) {
        if( true /* 330 */ ) {
            target.graphics.clear()
            gle.setTarget(target)

            // DEBUG
//            var cycle = 0.01f
//            for( i in 0 until states.length) {
//                states.wf[i] = when {
//                    cycle % 2f < 1f -> cycle % 1f
//                    else -> 1 - (cycle % 1f)
//                }
//                cycle += 0.01f
//            }
            // DEBUG

            val rgb = Vec3f(1f, 1f, 1f)
            val dot = linePassGeom(states, lineWidth)

            gle.applyPrimitiveProgram(PolyRenderCall(rgb, 1f), dot.circles, params, trans)
            gle.applyPrimitiveProgram(StrokeV2LinePass(rgb),dot.lines, params, trans)
            // Outer Edge Pass
            gle.applyPrimitiveProgram(StrokeV3LinePass(),dot.smallLines, params, trans)
//
//            val primitives = linePassGeom(states, lineWidth)
//
//            // Inner Poly Pass
//            gle.applyPrimitiveProgram(PolyRenderCall(rgb, 1f),
//                    primitives.second, params, trans)
//            // Outer Edge Pass
//            gle.applyPrimitiveProgram(StrokeV3LinePass(),
//                    primitives.first, params, trans)
        }
    }

    class DotPrimitive(
            val smallLines: GLPrimitive,
            val circles: GLPrimitive,
            val lines: GLPrimitive,
            val joints: GLPrimitive)

    companion object {
        val MAX_ERROR = 0.2f


        fun linePassGeom( states: DrawPoints, width: Float) : DotPrimitive {
            val smallLineBuilder = PrimitiveBuilder(intArrayOf(2, 1), GLC.LINE_STRIP)
            val circleBuilder = PrimitiveBuilder(intArrayOf(2), GLC.TRIANGLE_FAN)
            val lineBuilder = PrimitiveBuilder(intArrayOf(2), GLC.LINE_STRIP)
            val polyBuilder = PrimitiveBuilder(intArrayOf(2), GLC.TRIANGLE_STRIP)

            var inSmall = false

            for( i in (0 until states.length)) {
                val w = width * states.w[i]

                if( w < 1) {
                    if( i == 0) continue
                    smallLineBuilder.emitVertex(floatArrayOf(states.x[i], states.y[i], w))
                    inSmall = true
                }
                else {
                    if( inSmall) {
                        smallLineBuilder.emitPrimitive()
                    }

                    val r = (w)/2
                    val c = 1 - Math.abs(MAX_ERROR) / r
                    val theta_d = when {
                        c<0 -> PI/2.0
                        else -> FastMath.acos(c.d)
                    }

                    val cx = states.x[i]
                    val cy = states.y[i]
                    circleBuilder.emitVertex(floatArrayOf(cx, cy))

                    var theta = 0.0
                    val f2 = FloatArray(2)
                    while (theta < 2*PI) {
                        f2[0]  = (cx + r*FastMath.cos(theta)).f
                        f2[1] =  (cy + r*FastMath.sin(theta)).f
                        circleBuilder.emitVertex(f2)
                        lineBuilder.emitVertex(f2)

                        theta += theta_d
                    }
                    f2[0] = cx + r
                    f2[1] = cy
                    circleBuilder.emitVertex(f2)
                    lineBuilder.emitVertex(f2)
                    circleBuilder.emitPrimitive()
                    lineBuilder.emitPrimitive()

                    inSmall = false
                }
            }

            smallLineBuilder.emitPrimitive()
            circleBuilder.emitPrimitive()
            lineBuilder.emitPrimitive()
            polyBuilder.emitPrimitive()

            return DotPrimitive(
                    smallLineBuilder.build(),
                    circleBuilder.build(),
                    lineBuilder.build(),
                    polyBuilder.build())
        }
    }
}