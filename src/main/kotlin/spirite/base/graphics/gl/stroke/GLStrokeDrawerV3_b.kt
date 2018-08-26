package spirite.base.graphics.gl.stroke

import com.hackoeur.jglm.support.FastMath
import spirite.base.graphics.gl.*
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
import spirite.base.util.d
import spirite.base.util.f
import spirite.base.util.glu.GLC
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec3
import kotlin.math.PI

// Dot
class GLStrokeDrawerV3_b(
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

    override fun getIntensifyMethod(params: StrokeParams): StrokeV2ApplyCall.IntensifyMethod = when {
        params.hard -> StrokeV2ApplyCall.IntensifyMethod.HARD_EDGED
        else -> StrokeV2ApplyCall.IntensifyMethod.DEFAULT
    }

    private fun drawStroke(target: GLImage, states: DrawPoints, lineWidth: Float, params: GLParameters, strokeParams: StrokeParams, trans: Transform? = null) {
        if( true /* 330 */ ) {
            target.graphics.clear()
            gle.setTarget(target)

            // DEBUG
//            var cycle = 0.01f
//            for( i in 0 until states.length) {
//                states.w[i] = when {
//                    cycle % 2f < 1f -> cycle % 1f
//                    else -> 1 - (cycle % 1f)
//                }
//                cycle += 0.01f
//            }
            // DEBUG

            val rgb = Vec3(1f, 1f, 1f)
            val dot = linePassGeom(states, lineWidth)

//            gle.applyPrimitiveProgram(PolyRenderCall(rgb, 1f), dot.circles, params, trans)
            gle.applyPrimitiveProgram(StrokeV2LinePass(rgb),dot.lines, params, trans)
            // Outer Edge Pass
            gle.applyPrimitiveProgram(StrokeV3LinePass(),dot.smallLines, params, trans)

            gle.applyPrimitiveProgram(PolyRenderCall(rgb,1f), dot.circles, params, trans)
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


        fun linePassGeom(states: DrawPoints, width: Float) : DotPrimitive {
            val smallLineBuilder = PrimitiveBuilder(intArrayOf(2,1), GLC.LINE_STRIP)
            val circleBuilder = PrimitiveBuilder(intArrayOf(2), GLC.TRIANGLE_FAN)
            val lineBuilder = PrimitiveBuilder(intArrayOf(2), GLC.LINE_STRIP)
            val polyBuilder = PrimitiveBuilder(intArrayOf(2), GLC.TRIANGLE_STRIP)

            var inSmall = false

            val f2 = FloatArray(2)
            val f3 = FloatArray(3)

            for( i in (0 until states.length)) {
                val w = width * states.w[i]

                if( w < 1) {
                    if( i == 0) continue
                    f3[0] = states.x[i] ; f3[1] = states.y[i] ; f3[2] = w
                    smallLineBuilder.emitVertex(f3)

                    if(!inSmall && i > 2) {
                        val r0 = (width * states.w[i-1]-1f)/2
                        val p0 = Vec2(states.x[i-1], states.y[i-1])
                        val p1 = Vec2(states.x[i], states.y[i])
                        val dif = (p1-p0).normalize()*r0


                        f2[0] = p0.x - dif.y ; f2[1] = p0.y - dif.x
                        lineBuilder.emitVertex(f2)
                        f2[0] = p1.x ; f2[1] = p1.y
                        lineBuilder.emitVertex(f2)
                        f2[0] = p0.x + dif.y ; f2[1] = p0.y - dif.x
                        lineBuilder.emitVertex(f2)
                        lineBuilder.emitPrimitive()
                    }
                    inSmall = true
                }
                else {
                    if( inSmall) {
                        smallLineBuilder.emitPrimitive()
                    }

                    val r = (w-1f)/2
                    val c = 1 - Math.abs(MAX_ERROR) / r
                    val theta_d = when {
                        c<0 -> PI /2.0
                        else -> FastMath.acos(c.d)
                    }

                    val cx = states.x[i]
                    val cy = states.y[i]
                    circleBuilder.emitVertex(floatArrayOf(cx, cy))

                    var theta = 0.0
                    while (theta < 2* PI) {
                        f2[0]  = (cx + r* FastMath.cos(theta)).f
                        f2[1] =  (cy + r* FastMath.sin(theta)).f
                        circleBuilder.emitVertex(f2)
                        lineBuilder.emitVertex(f2)

                        theta += theta_d
                    }
                    f2[0] = cx + r
                    f2[1] = cy
                    circleBuilder.emitVertex(f2)
                    lineBuilder.emitVertex(f2)
                    lineBuilder.emitPrimitive()
                    circleBuilder.emitPrimitive()

                    if( i != 0) {

                        if (inSmall) {
                            val p0 = Vec2(states.x[i-1], states.y[i-1])
                            val p1 = Vec2(states.x[i], states.y[i])
                            val dif = (p1-p0).normalize()


                            f2[0] = p1.x - dif.y*r ; f2[1] = p1.y - dif.x*r
                            lineBuilder.emitVertex(f2)
                            f2[0] = p0.x ; f2[1] = p0.y
                            lineBuilder.emitVertex(f2)
                            f2[0] = p1.x + dif.y*r ; f2[1] = p1.y - dif.x*r
                            lineBuilder.emitVertex(f2)
                            lineBuilder.emitPrimitive()
                        } else {
                            val r0 = (width * states.w[i-1]-1f)/2
                            val p0 = Vec2(states.x[i-1], states.y[i-1])
                            val p1 = Vec2(states.x[i], states.y[i])
                            val dif = (p1-p0).normalize()

                            f2[0] = p0.x - dif.y*r0 ; f2[1] = p0.y - dif.x*r0
                            lineBuilder.emitVertex(f2)
                            f2[0] = p1.x - dif.y*r ; f2[1] = p1.y - dif.x*r
                            lineBuilder.emitVertex(f2)
                            lineBuilder.emitPrimitive()

                            f2[0] = p0.x + dif.y*r0 ; f2[1] = p0.y - dif.x*r0
                            lineBuilder.emitVertex(f2)
                            f2[0] = p1.x + dif.y*r ; f2[1] = p1.y - dif.x*r
                            lineBuilder.emitVertex(f2)
                            lineBuilder.emitPrimitive()
                        }
                    }

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