package spirite.base.graphics

import rb.glow.IImage
import rb.vectrix.linear.ITransformF
import spirite.base.graphics.CapMethod.NONE
import spirite.base.graphics.JoinMethod.MITER
import rb.glow.color.Color
import spirite.base.util.shapes.IShape
import java.util.*

/**
 * GraphicsContext is an abstract class which wraps all graphical functionality
 * which can be rendered by different native engines (e.g. either OpenGL or the
 * basic Java AWT methods) depending on settings/device capability.
 *
 * @author Rory Burks
 */
abstract class GraphicsContext {
    abstract val width: Int
    abstract val height: Int

    abstract var transform: ITransformF
    abstract var alpha: Float
    abstract var composite: Composite
    abstract var color: Color
    abstract var lineAttributes: LineAttributes

    abstract fun drawBounds(image: IImage, c: Int)  // TODO: Remove

    abstract fun clear( color: Color? = null)
    abstract fun preTranslate(offsetX: Float, offsetY: Float)
    abstract fun translate(offsetX: Float, offsetY: Float)
    abstract fun preTransform(trans: ITransformF)
    abstract fun transform(trans: ITransformF)
    abstract fun preScale(sx: Float, sy: Float)
    abstract fun scale(sx: Float, sy: Float)


    abstract fun drawRect(x: Int, y: Int, w: Int, h: Int)
    abstract fun drawOval(x: Int, y: Int, w: Int, h: Int)
    abstract fun drawPolyLine(x: IntArray, y: IntArray, count: Int)
    open fun drawPolyLine(x: FloatArray, y: FloatArray, count: Int) {
        val _x = IntArray(count)
        val _y = IntArray(count)
        for (i in 0 until count) {
            _x[i] = x[i].toInt()
            _y[i] = y[i].toInt()
        }
        drawPolyLine(_x, _y, count)
    }

    abstract fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float)
    abstract fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int)
    abstract fun draw(shape: IShape)

    abstract fun fillRect(x: Int, y: Int, w: Int, h: Int)
    abstract fun fillOval(x: Int, y: Int, w: Int, h: Int)
    abstract fun fill( shape: IShape)

    abstract fun fillPolygon(x: List<Float>, y: List<Float>, length: Int)

    abstract fun setClip(i: Int, j: Int, width: Int, height: Int)

    /** Marks the Graphic Context as no longer being used.  Strictly speaking,
     * calling this method shouldn't be necessary, but that relies on native
     * implementations of AWT's Graphics workspace.
     */
    abstract fun dispose()


    abstract fun renderImage(rawImage: IImage, x: Int, y: Int, render: RenderRubric? = null)


    private val transformStack = Stack<ITransformF>()

    fun pushTransform() {transformStack.push(transform.toMutable())}
    fun popTransform() {transform = transformStack.pop()}

    private val stateStack = Stack<GraphicalState>()
    fun pushState() { stateStack.push( GraphicalState(transform, composite, alpha, color))}
    fun popState() {
        val state = stateStack.pop()
        transform = state.trans
        composite = state.composite
        alpha = state.alpha
        color = state.color
    }

    open fun drawTransparencyBG( x: Int, y: Int, w: Int, h: Int, squareSize: Int) {}

}


enum class Composite {
    SRC, SRC_IN, SRC_OVER, SRC_OUT, SRC_ATOP,
    DST, DST_IN, DST_OVER, DST_OUT, DST_ATOP,
    CLEAR, XOR
}

enum class JoinMethod {MITER, ROUNDED, BEVEL}

enum class CapMethod {NONE, ROUND, SQUARE}

class LineAttributes (
        val width: Float,
        val cap: CapMethod = NONE,
        val join: JoinMethod = MITER,
        val dashes: FloatArray? = null)

data class GraphicalState(
        val trans: ITransformF,
        val composite: Composite,
        val alpha: Float,
        val color: Color)
