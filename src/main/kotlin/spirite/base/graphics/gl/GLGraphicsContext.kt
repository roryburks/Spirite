package spirite.base.graphics.gl

import spirite.base.graphics.*
import spirite.base.graphics.GraphicsContext.Composite.SRC_OVER
import spirite.base.graphics.RenderMethodType.*
import spirite.base.graphics.gl.RenderCall.RenderAlgorithm
import spirite.base.graphics.gl.RenderCall.RenderAlgorithm.STRAIGHT_PASS
import spirite.base.imageData.MediumHandle
import spirite.base.util.Color
import spirite.base.util.Colors
import spirite.base.util.glu.GLC
import spirite.base.util.glu.PolygonTesselater
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.hybrid.ImageConverter
import java.awt.Shape


class GLGraphicsContext : GraphicsContext {

    val image : GLImage?
    val premultiplied : Boolean
    private val cachedParams = GLParameters(1, 1)

    override var width: Int
        private set(value) {
            cachedParams.width = value
            field = value
        }
    override var height: Int
        private set(value) {
            cachedParams.heigth = value
            field = value
        }

    val gle: GLEngine

    constructor( width: Int, height: Int, flip: Boolean, gle:GLEngine)  {
        this.width = width
        this.height = height
        this.image = null
        this.gle = gle
        this.premultiplied = false

        cachedParams.premultiplied = this.premultiplied
    }
    constructor( glImage: GLImage)  {
        this.width = glImage.width
        this.height = glImage.height
        this.image = glImage
        this.gle = glImage.engine
        this.premultiplied = glImage.premultiplied

        cachedParams.premultiplied = this.premultiplied
    }

    private fun reset() = gle.setTarget(image)



    override fun drawBounds(image: IImage, c: Int) {
        val buffer = GLImage(width, height, gle)

        val gc = buffer.graphics
        gc.clear()

        val texture = ImageConverter(gle).convert<GLImage>(image)
        val bufferParams = cachedParams.copy( texture1 = texture)
        gc.applyPassProgram( BasicCall(),
                bufferParams, transform, 0f, 0f, image.width + 0f, image.height + 0f)

        bufferParams.texture1 = buffer
        applyPassProgram( BorderCall(c), bufferParams, null)

        buffer.flush()
    }

    override fun clear() {
        reset()
        gle.gl.clearColor( 0f, 0f, 0f, 0f)
        gle.gl.clear(GLC.COLOR)
    }

    // region Transforms
    override var transform: Transform
        get() = _trans
        set(value) {_trans = value.toMutable()}
    private var _trans : MutableTransform = MutableTransform.IdentityMatrix()

    override fun preTranslate(offsetX: Double, offsetY: Double) = _trans.preTranslate(offsetX.toFloat(), offsetY.toFloat())
    override fun translate(offsetX: Double, offsetY: Double) = _trans.translate(offsetX.toFloat(), offsetY.toFloat())

    override fun preTransform(trans: Transform) = _trans .preConcatenate(trans)
    override fun transform(trans: Transform) = _trans.concatenate(trans)

    override fun preScale(sx: Double, sy: Double) = _trans.preScale(sx.toFloat(), sy.toFloat())
    override fun scale(sx: Double, sy: Double) = _trans.scale(sx.toFloat(), sy.toFloat())
    // endregion

    // region Other Settings

    override var color : Color = Colors.BLACK

    override fun setComposite(composite: Composite, alpha: Float) {
        this.alpha = alpha
        this.composite = composite
    }
    override var alpha = 1f ; private set
    override var composite = SRC_OVER
        private set(value) {
            setCompositeBlend(cachedParams, value)
            field = value
        }
    private fun setCompositeBlend( params: GLParameters, composite: Composite) {
        when (composite) {
            GraphicsContext.Composite.SRC -> params.setBlendMode(GLC.ONE, GLC.ZERO, GLC.FUNC_ADD)
            GraphicsContext.Composite.SRC_OVER -> params.setBlendMode(GLC.ONE, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD)
            GraphicsContext.Composite.SRC_IN -> params.setBlendMode(GLC.DST_ALPHA, GLC.ZERO, GLC.FUNC_ADD)
            GraphicsContext.Composite.SRC_ATOP -> params.setBlendMode(GLC.DST_ALPHA, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD)
            GraphicsContext.Composite.SRC_OUT -> params.setBlendMode(GLC.ONE_MINUS_DST_ALPHA, GLC.ZERO, GLC.FUNC_ADD)

            GraphicsContext.Composite.DST -> params.setBlendMode(GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
            GraphicsContext.Composite.DST_OVER -> params.setBlendMode(GLC.ONE_MINUS_DST_ALPHA, GLC.ONE, GLC.FUNC_ADD)
            GraphicsContext.Composite.DST_IN -> params.setBlendMode(GLC.ZERO, GLC.SRC_ALPHA, GLC.FUNC_ADD)
            GraphicsContext.Composite.DST_ATOP -> params.setBlendMode(GLC.ONE_MINUS_DST_ALPHA, GLC.SRC_ALPHA, GLC.FUNC_ADD)
            GraphicsContext.Composite.DST_OUT -> params.setBlendMode(GLC.ZERO, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD)

            GraphicsContext.Composite.XOR -> params.setBlendMode(GLC.ONE_MINUS_DST_ALPHA, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD)
            GraphicsContext.Composite.CLEAR -> params.setBlendMode(GLC.ZERO, GLC.ZERO, GLC.FUNC_ADD)
        }
    }

    private val defaultLA = LineAttributes(1f, CapMethod.NONE, JoinMethod.ROUNDED, null)
    override var lineAttributes: LineAttributes = defaultLA

    override fun setClip(i: Int, j: Int, width: Int, height: Int) {
        cachedParams.clipRect = Rect( i, j, width, height)
    }

    // endregion

    // region Line Draws
    override fun drawRect(x: Int, y: Int, w: Int, h: Int) {
        reset()

        val x_ = floatArrayOf(x + 0f, x + w + 0f, x + w + 0f, x + 0f).toList()
        val y_ = floatArrayOf(y + 0f, y + 0f, y + h + 0f, y + h + 0f).toList()

        gle.applyComplexLineProgram(
                x_, y_, 4,
                lineAttributes.cap, lineAttributes.join,
                true, lineAttributes.width,
                color.rgbComponent, alpha,
                cachedParams, _trans)
    }

    override fun drawOval(x: Int, y: Int, w: Int, h: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawPolyLine(x: IntArray, y: IntArray, count: Int) {
        reset()
        gle.applyComplexLineProgram(
                x.map { it.toFloat() }, y.map { it.toFloat() }, count, lineAttributes.cap, lineAttributes.join,
                false, lineAttributes.width, color.rgbComponent, alpha, cachedParams, _trans)
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        reset()
        gle.applyComplexLineProgram( listOf(x1, x2), listOf(y1, y2), 2, lineAttributes.cap, lineAttributes.join,
                false, lineAttributes.width, color.rgbComponent, alpha, cachedParams, _trans)
    }

    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) = drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

    override fun draw(shape: Shape) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    // endregion

    // region Fills
    override fun fillRect(x: Int, y: Int, w: Int, h: Int) {
        reset()

        val x_ = floatArrayOf(x + 0f, x + w + 0f, x + 0f, x+ w + 0f).toList()
        val y_ = floatArrayOf(y + 0f, y + 0f, y + h + 0f, y + h + 0f).toList()

        gle.applyPolyProgram( PolyRenderCall(color.rgbComponent, alpha), x_, y_, 4,
                PolyType.STRIP, cachedParams, _trans)
    }

    override fun fillOval(x: Int, y: Int, w: Int, h: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fillPolygon(x: List<Float>, y: List<Float>, length: Int) {
        reset()
        val poly = PolygonTesselater.tesselatePolygon(x, y, x.size)
        gle.applyPrimitiveProgram( PolyRenderCall(color.rgbComponent, alpha), poly, cachedParams, _trans)
    }
    // endregion

    // region Images

    override fun dispose() { if( gle.target == image?.tex) gle.target = null}

    override fun renderImage(rawImage: IImage, x: Int, y: Int, render: RenderProperties) {
        val params = this.cachedParams.copy()

        val renderAlgorithm = when (render.method.methodType) {
            COLOR_CHANGE_HUE -> {
                setCompositeBlend(params, SRC_OVER)
                RenderAlgorithm.AS_COLOR
            }
            COLOR_CHANGE_FULL -> {
                setCompositeBlend(params, GraphicsContext.Composite.SRC_OVER)
                RenderAlgorithm.AS_COLOR_ALL
            }
            DISOLVE -> {
                setCompositeBlend(params, GraphicsContext.Composite.SRC_OVER)
                RenderAlgorithm.DISOLVE
            }
            LIGHTEN -> {
                params.setBlendModeExt(
                        GLC.ONE, GLC.ONE, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                RenderAlgorithm.STRAIGHT_PASS
            }
            SUBTRACT -> {
                params.setBlendModeExt(
                        GLC.ZERO, GLC.ONE_MINUS_SRC_COLOR, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                RenderAlgorithm.STRAIGHT_PASS
            }
            MULTIPLY -> {
                params.setBlendModeExt(GLC.DST_COLOR, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                RenderAlgorithm.STRAIGHT_PASS
            }
            SCREEN -> {
                // C = (1 - (1-DestC)*(1-SrcC) = SrcC*(1-DestC) + DestC
                params.setBlendModeExt(GLC.ONE_MINUS_DST_COLOR, GLC.ONE, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                RenderAlgorithm.STRAIGHT_PASS
            }
            OVERLAY -> RenderAlgorithm.DISOLVE
            DEFAULT -> {
                setCompositeBlend(params, GraphicsContext.Composite.SRC_OVER)
                RenderAlgorithm.STRAIGHT_PASS
            }
        }

        params.texture1 = ImageConverter(gle).convert<GLImage>(rawImage)
        applyPassProgram( RenderCall( alpha, render.method.renderValue, false, renderAlgorithm),
                params, transform, x + 0f, y + 0f, x + rawImage.width + 0f, y +  rawImage.height + 0f)
    }

    // endregion

    // region Direct
    // Note: These exist mostly to make sure Reset is called
    fun applyPassProgram( programCall: ProgramCall, params: GLParameters, trans: Transform?,
                          x1: Float = 0f, y1: Float = 0f, x2: Float = width.toFloat(), y2: Float = height.toFloat())
    {
        reset()
        gle.applyPassProgram( programCall, params, trans,  x1, y1, x2, y2)
    }

    // endregion

}