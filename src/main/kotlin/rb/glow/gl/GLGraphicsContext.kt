package rb.glow.gl

import rb.glow.*
import rb.glow.Composite.SRC_OVER
import rb.glow.gl.shader.programs.IGlProgramCall
import rb.glow.gl.shader.programs.PolyRenderCall
import rb.glow.gl.shader.programs.RenderCall
import rb.glow.gle.GLParameters
import rb.glow.gle.IGLEngine
import rb.glow.gle.RenderMethodType
import rb.glow.gle.RenderRubric
import rb.glow.img.IImage
import rb.vectrix.linear.ITransform
import rb.vectrix.mathUtil.f
import rb.vectrix.shapes.Rect
import rb.vectrix.shapes.RectI

class GLGraphicsContext : AGraphicsContext {
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

    val gle: IGLEngine

    constructor(width: Int, height: Int, flip: Boolean, gle: IGLEngine, premultiplied: Boolean = false)  {
        this.width = width
        this.height = height
        this.image = null
        this.gle = gle
        this.premultiplied = premultiplied

        cachedParams.premultiplied = this.premultiplied
        cachedParams.flip = flip
    }
    constructor( glImage: GLImage, flip: Boolean = false)  {
        this.width = glImage.width
        this.height = glImage.height
        this.image = glImage
        this.gle = glImage.engine
        this.premultiplied = glImage.premultiplied

        cachedParams.premultiplied = this.premultiplied
        cachedParams.flip = flip
    }

    private fun reset() {
        gle.setTarget(image)
    }

    override var color: Color = Colors.BLACK
    override var alpha = 1f
    override var composite: Composite = SRC_OVER
    override var lineAttributes: LineAttributes = LineAttributes(1f)

    private fun setCompositeBlend(params: GLParameters, composite: Composite) {
        when (composite) {
            Composite.ADD -> params.setBlendMode(GLC.ONE, GLC.ONE, GLC.FUNC_ADD)
            Composite.SRC -> params.setBlendMode(GLC.ONE, GLC.ZERO, GLC.FUNC_ADD)
            Composite.SRC_OVER -> params.setBlendMode(GLC.ONE, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD)
            Composite.SRC_IN -> params.setBlendMode(GLC.DST_ALPHA, GLC.ZERO, GLC.FUNC_ADD)
            Composite.SRC_ATOP -> params.setBlendMode(GLC.DST_ALPHA, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD)
            Composite.SRC_OUT -> params.setBlendMode(GLC.ONE_MINUS_DST_ALPHA, GLC.ZERO, GLC.FUNC_ADD)

            Composite.DST -> params.setBlendMode(GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
            Composite.DST_OVER -> params.setBlendMode(GLC.ONE_MINUS_DST_ALPHA, GLC.ONE, GLC.FUNC_ADD)
            Composite.DST_IN -> params.setBlendMode(GLC.ZERO, GLC.SRC_ALPHA, GLC.FUNC_ADD)
            Composite.DST_ATOP -> params.setBlendMode(GLC.ONE_MINUS_DST_ALPHA, GLC.SRC_ALPHA, GLC.FUNC_ADD)
            Composite.DST_OUT -> params.setBlendMode(GLC.ZERO, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD)

            Composite.XOR -> params.setBlendMode(GLC.ONE_MINUS_DST_ALPHA, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD)
            Composite.CLEAR -> params.setBlendMode(GLC.ZERO, GLC.ZERO, GLC.FUNC_ADD)
        }
    }


    override fun clear(color: Color) {
        val gl = gle.gl
        reset()
        //gl.clearColor(color.red, color.green, color.blue, color.alpha, GLC.COLOR_BUFFER_BIT)
        gl.clearColor(color.red, color.green, color.blue, color.alpha, GLC.COLOR)
    }

    /**
     * The way this constructs its call from the RenderRubric is:
     *  -performs all calls which require sharer algorithms in the order that they're entered
     *  -uses the BlendMode of the LAST BlendMode-related
     *
     * Essentially it makes sense to draw something as "Color-changed to Red, Disolved, and Multiply the result to the
     * screen", but it doesn't make a lot of sense (or rather the algorithms aren't in place) for it to "multiply and subtract
     * the texture from the screen"
     */
    override fun renderImage(image: IImage, x: Double, y: Double, render: RenderRubric?, imgPart: Rect? ) {
        val image = image as? GLImage ?: gle.converter.convertToGL(image, gle)
        val params = this.cachedParams.copy()

        val calls = mutableListOf<Pair<RenderCall.RenderAlgorithm,Int>>()

        // Default Blend Mode (may be over-written)
        setCompositeBlend(params, composite)

        // Construct Call Attributes from RenderRubric
        render?.methods?.forEach {
            when(it.methodType) {
                RenderMethodType.COLOR_CHANGE_HUE -> calls.add( Pair(RenderCall.RenderAlgorithm.AS_COLOR, it.renderValue))
                RenderMethodType.COLOR_CHANGE_FULL -> calls.add( Pair(RenderCall.RenderAlgorithm.AS_COLOR_ALL, it.renderValue))
                RenderMethodType.DISOLVE -> calls.add(Pair(RenderCall.RenderAlgorithm.DISSOLVE, it.renderValue))
                RenderMethodType.LIGHTEN -> params.setBlendModeExt(
                        GLC.ONE, GLC.ONE, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                RenderMethodType.SUBTRACT -> params.setBlendModeExt(
                        GLC.ZERO, GLC.ONE_MINUS_SRC_COLOR, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                RenderMethodType.MULTIPLY -> params.setBlendModeExt(
                        GLC.DST_COLOR, GLC.ONE_MINUS_SRC_ALPHA, GLC.FUNC_ADD,
                        GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                RenderMethodType.SCREEN ->
                    // C = (1 - (1-DestC)*(1-SrcC) = SrcC*(1-DestC) + DestC
                    params.setBlendModeExt(
                            GLC.ONE_MINUS_DST_COLOR, GLC.ONE, GLC.FUNC_ADD,
                            GLC.ZERO, GLC.ONE, GLC.FUNC_ADD)
                RenderMethodType.DEFAULT -> {}
            }
        }

        params.texture1 = image

        val tDraw = when( render) {
            null -> transform
            else -> transform * render.transform
        }
        reset()

        if( imgPart == null) {
            gle.applyPassProgram(
                    RenderCall(alpha * (render?.alpha ?: 1f), calls),
                    params, tDraw, (x + 0).f, (y + 0).f, (x + image.width).f, (y + image.height).f
            )
        }
        else {
            gle.applyPassProgram(
                    RenderCall(alpha * (render?.alpha ?: 1f), calls), params, tDraw,
                    (x + 0).f, (y).f, (x+imgPart.w).f, (y+imgPart.h).f,
                    (imgPart.x1 / image.width).f, (imgPart.y1 / image.height).f, (imgPart.x2 / image.width).f, (imgPart.y2 / image.height).f
            )
        }
    }

    override fun drawPolyLine(x: Iterable<Double>, y: Iterable<Double>, count: Int, loop: Boolean) {
        reset()
        gle.applyComplexLineProgram(
                x.map { it.f }, y.map { it.f }, count,
                lineAttributes.cap,
                lineAttributes.join,
                false,
                lineAttributes.width,
                color.rgbComponent,
                alpha,
                cachedParams,
                transform)
    }

    override fun fillPolygon(x: Iterable<Double>, y: Iterable<Double>, count: Int) {
        val poly = gle.tesselator.tesselatePolygon(x.asSequence(), y.asSequence(), count)
        reset()
        gle.applyPrimitiveProgram(
                PolyRenderCall(
                        color.rgbComponent,
                        alpha
                ), poly, cachedParams, transform)
    }

    override fun setClip(i: Int, j: Int, width: Int, height: Int) {
        cachedParams.clipRect = RectI( i, j, width, height)
    }

    fun applyPassProgram(
            call: IGlProgramCall,
            params: GLParameters,
            trans: ITransform?,
            x: Float = 0f, y: Float = 0f, w: Float = width.f, h: Float = height.f)
    {
        reset()
        gle.applyPassProgram(call, params, trans, x, y, x + w, y + h)
    }

    fun applyPassProgram(
            programCall: IGlProgramCall,
            image: GLImage,
            x1 :Float= 0f, y1 :Float= 0f, x2 : Float = image.width.f, y2 : Float = image.height.f)
    {
        reset()
        gle.applyPassProgram( programCall, cachedParams.copy(texture1 = image), transform, x1, y1, x2, y2)

    }
}