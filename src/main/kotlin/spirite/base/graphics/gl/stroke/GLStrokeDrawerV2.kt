package spirite.base.graphics.gl.stroke

import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod.DEFAULT
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod.HARD_EDGED
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
import spirite.base.util.glu.GLC
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec3

class GLStrokeDrawerV2(
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

    override fun getIntensifyMethod(params: StrokeParams): IntensifyMethod = when {
        params.hard -> HARD_EDGED
        else -> DEFAULT
    }

    private fun drawStroke(target: GLImage, states: DrawPoints, lineWidth: Float, params: GLParameters, strokeParams: StrokeParams, trans: Transform? = null) {
        val vb = composeVBuffer( states, lineWidth)

        if( true /* 330 */ ) {
            target.graphics.clear()
            gle.setTarget(target)

            val primitives = strokeV2LinePassGeom(vb)
            val rgb = Vec3(1f, 1f, 1f)

            // Inner Poly Pass
            gle.applyPrimitiveProgram(PolyRenderCall(rgb, 1f),
                    primitives.second, params, trans)
            // Outer Edge Pass
            gle.applyPrimitiveProgram(StrokeV2LinePass(rgb),
                    primitives.first, params, trans)
        }
    }

    private val STRIDE = 3
    private fun composeVBuffer(states: DrawPoints, lineWidth: Float) : FloatArray {
        // Step 1: Determine how much space is needed
        val num = states.length + 2


        val raw = FloatArray(STRIDE * num)
        var o = 1    // first point is 0,0,0,0
        for (i in 0 until states.length) {
            val off = o++ * STRIDE

            // x y z w
            val xy = Vec2(states.x[i], states.y[i])
            //val xy = tMediumToWorkspace!!.apply(Vec2(states.x[i], states.y[i]))
            raw[off + 0] = xy.x
            raw[off + 1] = xy.y

            // size pressure
            raw[off + 2] = states.w[i] * lineWidth
            //			raw[off+3] = ps.pressure;

            /*			if( i == states.size()-1 && stroke.getMethod() == Method.PIXEL) {
				// TODO: Exagerate last line segment so pixel drawing works as expected
				raw[off+0] = data.convertX(ps.x)+0.5f;
				raw[off+1] = data.convertY(ps.y)+0.5f;
			}*/
        }

        raw[0] = raw[STRIDE]
        raw[1] = raw[STRIDE + 1]
        raw[(1 + states.length) * STRIDE] = raw[states.length * STRIDE]
        raw[(1 + states.length) * STRIDE + 1] = raw[states.length * STRIDE + 1]

        return raw
    }


    private val SV2_STRIDE = 3
    fun strokeV2LinePassGeom( raw: FloatArray) : Pair<GLPrimitive,GLPrimitive> {
        val lineBuilder = DoubleEndedSinglePrimitiveBuilder(intArrayOf(2), GLC.LINE_STRIP)
        val polyBuilder = PrimitiveBuilder(intArrayOf(2), GLC.TRIANGLE_STRIP)

        for( i in 0 until (raw.size / SV2_STRIDE) - 3) {
            val p0 = Vec2(raw[(i + 0) * SV2_STRIDE], raw[(i + 0) * SV2_STRIDE + 1])
            val p1 = Vec2(raw[(i + 1) * SV2_STRIDE], raw[(i + 1) * SV2_STRIDE + 1])
            val p2 = Vec2(raw[(i + 2) * SV2_STRIDE], raw[(i + 2) * SV2_STRIDE + 1])
            val p3 = Vec2(raw[(i + 3) * SV2_STRIDE], raw[(i + 3) * SV2_STRIDE + 1])
            val size1 = raw[(i + 1) * SV2_STRIDE + 2] / 2
            val size2 = raw[(i + 2) * SV2_STRIDE + 2] / 2
            //Vec2 n10 = p1.minus(p0).normalize();
            val (x, y) = p2.minus(p1).normalize()
            //Vec2 n32 = p3.minus(p2).normalize();

            if (p0 == p1) {
                lineBuilder.emitVertexFront(floatArrayOf(p1.x - x * size1 / 2, p1.y - y * size1 / 2))
                lineBuilder.emitVertexBack(floatArrayOf(p1.x - x * size1 / 2, p1.y - y * size1 / 2))

                //if( size1 > 0.5) {
                polyBuilder.emitVertex(floatArrayOf(p1.x - x * size1 / 2, p1.y - y * size1 / 2))
                polyBuilder.emitVertex(floatArrayOf(p1.x - x * size1 / 2, p1.y - y * size1 / 2))
                //}
                //else polyBuilder.emitPrimitive();
            } else {
                //                Vec2 tangent = p2.minus(p1).normalize().plus( p1.minus(p0).normalize()).normalize();
                //                Vec2 miter = new Vec2( -tangent.y, tangent.x);
                //                Vec2 n1 = (new Vec2( -(p1.y - p0.y), p1.x - p0.x)).normalize();


                val length = Math.max(0f, size1 - 0.5f)
                //                float length = Math.max( 0.5f, Math.min( MITER_MAX2*size1, size1 / miter.dot(n1)));

                val left = floatArrayOf(p1.x + (p2.y - p0.y) * length / 2, p1.y - (p2.x - p0.x) * length / 2)
                val right = floatArrayOf(p1.x - (p2.y - p0.y) * length / 2, p1.y + (p2.x - p0.x) * length / 2)

                lineBuilder.emitVertexFront(left)
                lineBuilder.emitVertexBack(right)
                //if( length > 0.5) {
                //float s = length;//-0.5f;
                polyBuilder.emitVertex(left)
                polyBuilder.emitVertex(right)
                //}
                //else polyBuilder.emitPrimitive();
                //
                //                lineBuilder.emitVertexFront(new float[] { miter.x*length + p1.x, miter.y*length + p1.y});
                //                lineBuilder.emitVertexBack(new float[] { -miter.x*length + p1.x, -miter.y*length + p1.y});
                //        		if( length > 0.5) {
                //        			float s = length;//-0.5f;
                //        			polyBuilder.emitVertex(new float[] { miter.x*s + p1.x, miter.y*s + p1.y});
                //        			polyBuilder.emitVertex(new float[] { -miter.x*s + p1.x, -miter.y*s + p1.y});
                //        		}
                //        		else polyBuilder.emitPrimitive();
            }
            if (p2 == p3) {
                val length = Math.max(0f, size2 - 0.5f)
                lineBuilder.emitVertexFront(floatArrayOf(p2.x + x * length / 2, p2.y + y * length / 2))
                lineBuilder.emitVertexBack(floatArrayOf(p2.x + x * length / 2, p2.y + y * length / 2))
                //if( size2 > 0.5) {
                polyBuilder.emitVertex(floatArrayOf(p2.x + x * size2 / 2, p2.y + y * size2 / 2))
                polyBuilder.emitVertex(floatArrayOf(p2.x + x * size2 / 2, p2.y + y * size2 / 2))
                //}
                polyBuilder.emitPrimitive()
            }
            /*else {
                Vec2 tangent = p3.minus(p2).normalize().plus( p2.minus(p1).normalize()).normalize();
                Vec2 miter = new Vec2( -tangent.y, tangent.x);
                Vec2 n2 = (new Vec2( -(p2.y - p1.y), p2.x - p1.x)).normalize();
                float length = Math.max( 0.5f, Math.min( MITER_MAX2, size2 / miter.dot(n2)));

                lineBuilder.emitVertexFront( new float[]{ miter.x*length + p2.x, miter.y*length + p2.y});
                lineBuilder.emitVertexBack( new float[]{ -miter.x*length + p2.x, -miter.y*length + p2.y});
        	}*/
        }

        return Pair( lineBuilder.build(), polyBuilder.build())
    }
}