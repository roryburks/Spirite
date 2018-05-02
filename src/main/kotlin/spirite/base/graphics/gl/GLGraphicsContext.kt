package spirite.base.graphics.gl

import spirite.base.graphics.*
import spirite.base.graphics.GraphicsContext.Composite.SRC_OVER
import spirite.base.graphics.RenderMethodType.*
import spirite.base.graphics.gl.RenderCall.RenderAlgorithm
import spirite.base.graphics.gl.RenderCall.RenderAlgorithm.*
import spirite.base.graphics.shapes.IShape
import spirite.base.graphics.shapes.Oval
import spirite.base.util.Color
import spirite.base.util.Colors
import spirite.base.util.f
import spirite.base.util.glu.GLC
import spirite.base.util.glu.PolygonTesselater
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.hybrid.ImageConverter


class GLGraphicsContext : GraphicsContext {

    val image : GLImage?
    val premultiplied : Boolean
    val cachedParams = GLParameters(1, 1)

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

    constructor( width: Int, height: Int, flip: Boolean, gle:GLEngine, premultiplied: Boolean = false)  {
        this.width = width
        this.height = height
        this.image = null
        this.gle = gle
        this.premultiplied = premultiplied

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

    private fun reset() {
        gle.setTarget(image)
    }



    override fun drawBounds(image: IImage, c: Int) {
        using(GLImage(width, height, gle)) {buffer ->
            val gc = buffer.graphics
            gc.clear()

            val texture = ImageConverter(gle).convert<GLImage>(image)
            val bufferParams = cachedParams.copy( texture1 = texture)
            gc.applyPassProgram( BasicCall(),
                    bufferParams, transform, 0f, 0f, image.width + 0f, image.height + 0f)

            bufferParams.texture1 = buffer
            applyPassProgram( BorderCall(c), bufferParams, null)
        }
    }

    override fun clear( color: Color?) {
        reset()
        val gl = gle.getGl()
        gl.clearColor( color?.red ?: 0f, color?.green ?: 0f, color?.blue ?: 0f, color?.alpha ?: 0f, GLC.COLOR)
    }

    // region Transforms
    override var transform: Transform
        get() = _trans
        set(value) {_trans = value.toMutable()}
    private var _trans : MutableTransform = MutableTransform.IdentityMatrix()

    override fun preTranslate(offsetX: Float, offsetY: Float) = _trans.preTranslate(offsetX.toFloat(), offsetY.toFloat())
    override fun translate(offsetX: Float, offsetY: Float) = _trans.translate(offsetX.toFloat(), offsetY.toFloat())

    override fun preTransform(trans: Transform) = _trans .preConcatenate(trans)
    override fun transform(trans: Transform) = _trans.concatenate(trans)

    override fun preScale(sx: Float, sy: Float) = _trans.preScale(sx.toFloat(), sy.toFloat())
    override fun scale(sx: Float, sy: Float) = _trans.scale(sx.toFloat(), sy.toFloat())
    // endregion

    // region Other Settings

    override var color : Color = Colors.BLACK

    override var alpha = 1f
    override var composite = SRC_OVER
        set(value) {
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
        draw( Oval(x + w/2.0f, y + h/2.0f, w/2.0f, h/2.0f))
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

    override fun draw(shape: IShape) {
        reset()

        val x_y = shape.buildPath(0.5f)

        gle.applyComplexLineProgram( x_y.first.asList(), x_y.second.asList(), x_y.first.size, lineAttributes.cap, lineAttributes.join,
                true, lineAttributes.width, color.rgbComponent, alpha, cachedParams, _trans)
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
        fill( Oval(x + w/2.0f, y + h/2.0f, w/2.0f, h/2.0f))
    }

    override fun fill(shape: IShape) {
        reset()
        val x_y = shape.buildPath(0.5f)
        gle.applyPolyProgram( PolyRenderCall(color.rgbComponent, alpha), x_y.first.asList(), x_y.second.asList(), x_y.first.size,
                PolyType.FAN, cachedParams, _trans)
    }

    override fun fillPolygon(x: List<Float>, y: List<Float>, length: Int) {
        reset()
        val poly = PolygonTesselater.tesselatePolygon(x, y, x.size)
        gle.applyPrimitiveProgram( PolyRenderCall(color.rgbComponent, alpha), poly, cachedParams, _trans)
    }
    // endregion

    // region Images

    override fun dispose() { if( gle.target == image?.tex) gle.target = null}

    /**
     * The way this constructs its call from the RenderRubric is:
     *  -performs all calls which require sharer algorithms in the order that they're entered
     *  -uses the BlendMode of the LAST BlendMode-related
     *
     * Essentially it makes sense to draw something as "Color-changed to Red, Disolved, and Multiply the result to the
     * screen", but it doesn't make a lot of sense (or rather the algorithms aren't in place) for it to "multiply and subtract
     * the texture from the screen"
     */
    override fun renderImage(rawImage: IImage, x: Int, y: Int, render: RenderRubric?) {
        val params = this.cachedParams.copy()

        val calls = mutableListOf<Pair<RenderAlgorithm,Int>>()

        // Default Blend Mode (may be over-written)
        setCompositeBlend(params, composite)

        // Construct Call Attributes from RenderRubric
        render?.methods?.forEach {
            when(it.methodType) {
                COLOR_CHANGE_HUE -> calls.add( Pair(AS_COLOR, it.renderValue))
                COLOR_CHANGE_FULL -> calls.add( Pair(AS_COLOR_ALL, it.renderValue))
                DISOLVE -> calls.add(Pair(DISSOLVE, it.renderValue))
                LIGHTEN -> params.setBlendModeExt(
                        GLC.ONE, GLC.ONE, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                SUBTRACT -> params.setBlendModeExt(
                        GLC.ZERO, GLC.ONE_MINUS_SRC_COLOR, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                MULTIPLY -> params.setBlendModeExt(
                        GLC.DST_COLOR, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                SCREEN ->
                    // C = (1 - (1-DestC)*(1-SrcC) = SrcC*(1-DestC) + DestC
                    params.setBlendModeExt(
                            GLC.ONE_MINUS_DST_COLOR, GLC.ONE, GLC.FUNC_ADD,
                            GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                DEFAULT -> {}
            }
        }

        params.texture1 = ImageConverter(gle).convert<GLImage>(rawImage)

        val tDraw = when( render) {
            null -> transform
            else -> transform * render.transform
        }

        applyPassProgram( RenderCall( alpha * (render?.alpha ?: 1f), calls),
                params, tDraw, x + 0f, y + 0f, x + rawImage.width + 0f, y +  rawImage.height + 0f)
    }

    // endregion

    override fun drawTransparencyBG(x: Int, y: Int, w: Int, h: Int, squareSize: Int) {
        applyPassProgram( GridCall( Colors.GRAY.rgbComponent, Colors.LIGHT_GRAY.rgbComponent, squareSize),
                cachedParams, transform, x.f, y.f, w.f, h.f)
    }

    // region Direct
    // Note: These exist mostly to make sure Reset is called
    fun applyPassProgram( programCall: ProgramCall, image: GLImage, x1 :Float= 0f, y1 :Float= 0f, x2 : Float = image.width.f, y2 : Float = image.height.f)
    {
        reset()
        gle.applyPassProgram( programCall, cachedParams.copy(texture1 = image), transform, x1, y1, x2, y2)

    }

    private fun applyPassProgram( programCall: ProgramCall, params: GLParameters, trans: Transform?,
                          x1: Float = 0f, y1: Float = 0f, x2: Float = width.f, y2: Float = height.f)
    {
        reset()
        gle.applyPassProgram( programCall, params, trans,  x1, y1, x2, y2)
    }

    // endregion

}