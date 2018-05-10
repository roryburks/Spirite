package spirite.base.graphics.gl.stroke

import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod.DEFAULT
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod.HARD_EDGED
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.StrokeParams
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec3

class GLStrokeDrawerV2(
        gle: GLEngine) : GLStrokeDrawer(gle)
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

            val primitives = GLGeom.strokeV2LinePassGeom(vb)
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
}