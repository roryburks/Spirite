package spirite.base.graphics.gl

import spirite.base.graphics.*
import spirite.base.graphics.GraphicsContext.Composite.SRC_OVER
import spirite.base.imageData.MediumHandle
import spirite.base.util.Colors
import spirite.base.util.glu.GLC
import spirite.base.util.glu.PolygonTesselater
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Vec3
import java.awt.Shape


class GLGraphics : GraphicsContext {

    val image : GLImage?

    override var width: Int
        private set(value) {
            params.width = value
            field = value
        }
    override var height: Int
        private set(value) {
            params.heigth = value
            field = value
        }

    val gle: GLEngine

    constructor( width: Int, height: Int, flip: Boolean, gle:GLEngine)  {
        this.width = width
        this.height = height
        image = null
        this.gle = gle
    }
    constructor( glImage: GLImage)  {
        width = glImage.width
        height = glImage.height
        image = glImage
        this.gle = glImage.engine
    }

    private fun reset() {
        gle.setTarget(image)
    }
    private val params = GLParametersMutable(1, 1)




    override fun drawBounds(bi: IImage, c: Int) {
        val img = GLImage(width, height, gle)

        val other = img.graphics
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        reset()
        gle.gl.clearColor( 0f, 0f, 0f, 0f)
        gle.gl.clear(GLC.COLOR)
    }

    // region Transform
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

    override var color = 0xffffffff.toInt()
        set(value) {
            field = value
            _color = null
        }
    private var _color : Vec3? = null
        get() {
            val f = field ?: Vec3( Colors.getRed(color)/255f,Colors.getBlue(color)/255f,Colors.getBlue(color)/255f)
            field = f
            return f
        }

    override fun setComposite(composite: Composite, alpha: Float) {
        this.alpha = alpha
        this.composite = composite
    }
    override var alpha = 1f ; private set
    override var composite = SRC_OVER
        private set(value) {
            when (value) {
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
                _color!!, alpha,
                params, _trans)
    }

    override fun drawOval(x: Int, y: Int, w: Int, h: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawPolyLine(x: IntArray, y: IntArray, count: Int) {
        reset()
        gle.applyComplexLineProgram(
                x.map { it.toFloat() }, y.map { it.toFloat() }, count, lineAttributes.cap, lineAttributes.join,
                false, lineAttributes.width, _color!!, alpha, params, _trans)
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        reset()
        gle.applyComplexLineProgram( listOf(x1, x2), listOf(y1, y2), 2, lineAttributes.cap, lineAttributes.join,
                false, lineAttributes.width, _color!!, alpha, params, _trans)
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

        gle.applyPolyProgram( PolyRenderCall(_color!!, alpha), x_, y_, 4,
                PolyType.STRIP, params, _trans)
    }

    override fun fillOval(x: Int, y: Int, w: Int, h: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun fillPolygon(x: List<Float>, y: List<Float>, length: Int) {
        reset()
        val poly = PolygonTesselater.tesselatePolygon(x, y, x.size)
        gle.applyPrimitiveProgram( PolyRenderCall(_color!!, alpha), poly, params, _trans)
    }
    // endregion


    override fun drawImage(img: IImage, x: Int, y: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun drawHandle(handle: MediumHandle, x: Int, y: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setClip(i: Int, j: Int, width: Int, height: Int) {
        params.clipRect = Rect( i, j, width, height)
    }

    override fun dispose() { if( gle.target == image?.tex) gle.target = null}

    override fun renderImage(rawImage: IImage, x: Int, y: Int, render: RenderProperties) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun renderHandle(handle: MediumHandle, x: Int, y: Int, render: RenderProperties) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}