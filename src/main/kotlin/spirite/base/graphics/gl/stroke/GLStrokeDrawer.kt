package spirite.base.graphics.gl.stroke

import spirite.base.brains.toolset.PenDrawMode.BEHIND
import spirite.base.brains.toolset.PenDrawMode.KEEP_ALPHA
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.Composite.*
import spirite.base.graphics.gl.*
import spirite.base.graphics.gl.StrokeV2ApplyCall.IntensifyMethod
import spirite.base.graphics.using
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.IStrokeDrawer
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.ERASE
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL



abstract class GLStrokeDrawer(val gle: IGLEngine)
    :IStrokeDrawer
{
    protected class DrawerContext (
            val builder: StrokeBuilder,
            val image: GLImage,
            val glParams: GLParameters)

    private var context: DrawerContext? = null

    protected abstract fun doStart(context: DrawerContext)
    protected abstract fun doStep(context: DrawerContext)
    protected abstract fun doBatch(image: GLImage, drawPoints: DrawPoints, params: StrokeParams, glParams: GLParameters)
    protected abstract fun getIntensifyMethod(params: StrokeParams) : IntensifyMethod

    override fun start(builder: StrokeBuilder, width: Int, height: Int): Boolean {
        val image = GLImage( width, height, gle, false)
        val glParams = GLParameters(width, height, premultiplied = false)

        gle.setTarget(image)
        context = DrawerContext(builder, image, glParams)
                .also { doStart(it)}
        return true
    }

    override fun step(): Boolean {
        val ctx = context
        return when( ctx) {
            null -> {
                MDebug.handleError(STRUCTURAL, "Tried to continue Stroke that isn't started.")
                false
            }
            else -> {
                doStep(ctx)
                true
            }
        }
    }

    override fun draw(gc: GraphicsContext) {
        context?.also { ctx -> drawStrokeImageToGc(ctx.image, gc, ctx.builder.params)}
    }

    override fun end() {
        context?.image?.flush()
        context = null
    }

    override fun batchDraw(gc: GraphicsContext, drawPoints: DrawPoints, params: StrokeParams, width: Int, height: Int) {
        using( GLImage(width, height, gle, false)) {batchImage ->
            val glParams = batchImage.glParams
            doBatch(batchImage, drawPoints, params, glParams)

            drawStrokeImageToGc(batchImage, gc, params)
        }
    }

    private fun drawStrokeImageToGc(image: GLImage, gc: GraphicsContext, strokeParams: StrokeParams) {
        val glgc = gc as? GLGraphicsContext ?: return

        glgc.pushState()

        when {
            strokeParams.method == ERASE -> glgc.composite = DST_OUT
            strokeParams.mode == BEHIND -> glgc.composite = DST_OVER
            strokeParams.mode == KEEP_ALPHA -> glgc.composite = SRC_ATOP
        }

        glgc.applyPassProgram(StrokeApplyCall(strokeParams.color.rgbComponent, strokeParams.alpha * gc.alpha), image)

        glgc.popState()
    }
}

