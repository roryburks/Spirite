package spirite.specialRendering.stroke

import rb.glow.Composite.*
import rb.glow.IGraphicsContext
import rb.glow.gl.GLGraphicsContext
import rb.glow.gl.GLImage
import rb.glow.gle.GLParameters
import rb.glow.gle.IGLEngine
import rb.glow.using
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.ITransformF
import spirite.sguiHybrid.MDebug
import spirite.sguiHybrid.MDebug.ErrorType.STRUCTURAL
import spirite.base.brains.toolset.PenDrawMode.BEHIND
import spirite.base.brains.toolset.PenDrawMode.KEEP_ALPHA
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.IStrokeDrawer
import spirite.base.pen.stroke.StrokeBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.base.pen.stroke.StrokeParams.Method.ERASE
import spirite.specialRendering.StrokeApplyCall
import spirite.specialRendering.StrokeV2ApplyCall.IntensifyMethod


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
    protected abstract fun doBatch(image: GLImage, drawPoints: DrawPoints, params: StrokeParams, glParams: GLParameters, transform: ITransform?)
    protected abstract fun getIntensifyMethod(params: StrokeParams) : IntensifyMethod

    override fun start(builder: StrokeBuilder, width: Int, height: Int): Boolean {
        val image = GLImage(width, height, gle, false)
        val glParams = GLParameters(width, height, premultiplied = false)

        gle.setTarget(image)
        context = DrawerContext(builder, image, glParams)
                .also { doStart(it)}
        return true
    }

    override fun step() = when( val ctx = context) {
        null -> {
            MDebug.handleError(STRUCTURAL, "Tried to continue Stroke that isn't started.")
            false
        }
        else -> {
            doStep(ctx)
            true
        }
    }

    override fun draw(gc: IGraphicsContext) {
        context?.also { ctx -> drawStrokeImageToGc(ctx.image, gc, ctx.builder.params)}
    }

    override fun end() {
        context?.image?.flush()
        context = null
    }

    override fun batchDraw(gc: IGraphicsContext, drawPoints: DrawPoints, params: StrokeParams, width: Int, height: Int) {
        using(GLImage(width, height, gle, false)) { batchImage ->
            val glParams = batchImage.glParams
            doBatch(batchImage, drawPoints, params, glParams, gc.transform)

            gc.pushTransform()
            gc.transform = ITransformF.Identity
            drawStrokeImageToGc(batchImage, gc, params)
            gc.popTransform()
        }
    }

    private fun drawStrokeImageToGc(image: GLImage, gc: IGraphicsContext, strokeParams: StrokeParams) {
        val glgc = gc as? GLGraphicsContext ?: return

        glgc.pushState()

        when {
            strokeParams.method == ERASE -> glgc.composite = DST_OUT
            strokeParams.mode == BEHIND -> glgc.composite = DST_OVER
            strokeParams.mode == KEEP_ALPHA -> glgc.composite = SRC_ATOP
        }
        println("${strokeParams.method} , ${strokeParams.mode} , ${glgc.composite}")

        glgc.applyPassProgram(StrokeApplyCall(strokeParams.color.rgbComponent, strokeParams.alpha * gc.alpha), image)

        glgc.popState()
    }
}

