package spirite.base.graphics.gl.stroke

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IImage
import spirite.base.graphics.gl.*
import spirite.base.pen.stroke.*
import spirite.base.util.Color
import spirite.base.util.Colors
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec2
import spirite.base.util.linear.Vec3
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

class GLStrokeDrawerV2(
        val gle: GLEngine
) : IStrokeDrawer {
    private class DrawerContext (
            val builder: StrokeBuilder,
            val image: GLImage,
            val glParams: GLParameters)
    private var context : DrawerContext? = null

    override fun start(builder: StrokeBuilder, width: Int, height: Int): Boolean {
        val image = GLImage( width, height, gle, false)
        val glParams = GLParameters(width, height, premultiplied = false)

        gle.setTarget(image)
        context = DrawerContext(builder, image, glParams)

        drawStroke( image, builder.currentPoints, builder.params.width, glParams)
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
                drawStroke(ctx.image, ctx.builder.currentPoints, ctx.builder.params.width, ctx.glParams)
                return true
            }
        }
    }

    override fun end() {
        context?.image?.flush()
        context = null
    }

    override fun batchDraw(drawPoints: DrawPoints, params: StrokeParams, width: Int, height: Int): GLImage {
        val batchImage = GLImage(width, height, gle, false)
        val glParams = batchImage.glParams
        drawStroke( batchImage, drawPoints, params.width, glParams)
        return batchImage
    }


    fun drawStroke(target: GLImage, states: DrawPoints, lineWidth: Float, params: GLParameters, trans: Transform? = null) {
        val vb = composeVBuffer( states, lineWidth)

        if( true /* 330 */ ) {
            gle.setTarget(target)

            val primitives = GLGeom.strokeV2LinePassGeom(vb)
            val rgb = Vec3(1f,1f,1f)

            // Inner Poly Pass
            gle.applyPrimitiveProgram( PolyRenderCall(rgb, 1f),
                    primitives.second, params, trans)
            // Outer Edge Pass
            gle.applyPrimitiveProgram( StrokeV2LinePass(rgb),
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