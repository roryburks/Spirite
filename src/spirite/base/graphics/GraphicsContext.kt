package spirite.base.graphics

import java.awt.Shape    // TODO
import java.util.Stack

import spirite.base.image_data.MediumHandle
import spirite.base.util.linear.MatTrans
import spirite.base.util.linear.Rect

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

    /** Setting to null produces undefined behavior.  */
    abstract var transform: MatTrans
    abstract val alpha: Float
    abstract val composite: Composite

    /** May return null if the underlying engine's Line Attributes aren't
     * representable by the generic LineAttributes class.  */
    abstract var lineAttributes: LineAttributes

    abstract fun drawBounds(bi: IImage, c: Int)

    abstract fun clear()
    abstract fun preTranslate(offsetX: Double, offsetY: Double)
    abstract fun translate(offsetX: Double, offsetY: Double)
    abstract fun preTransform(trans: MatTrans)
    abstract fun transform(trans: MatTrans)
    abstract fun preScale(sx: Double, sy: Double)
    abstract fun scale(sx: Double, sy: Double)
    abstract fun setColor(argb: Int)


    enum class Composite {
        SRC, SRC_IN, SRC_OVER, SRC_OUT, SRC_ATOP,
        DST, DST_IN, DST_OVER, DST_OUT, DST_ATOP,
        CLEAR, XOR
    }

    abstract fun setComposite(composite: Composite, alpha: Float)


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
    abstract fun draw(shape: Shape)

    abstract fun fillRect(x: Int, y: Int, w: Int, h: Int)
    abstract fun fillOval(x: Int, y: Int, w: Int, h: Int)


    abstract fun drawImage(img: IImage, x: Int, y: Int)
    abstract fun drawHandle(handle: MediumHandle, x: Int, y: Int)

    abstract fun fillPolygon(x: IntArray, y: IntArray, count: Int)
    abstract fun fillPolygon(x: FloatArray, y: FloatArray, length: Int)

    abstract fun setClip(i: Int, j: Int, width: Int, height: Int)

    /** Marks the Graphic Context as no longer being used.  Strictly speaking,
     * calling this method shouldn't be necessary, but that relies on native
     * implementations of AWT's Graphics context.
     */
    abstract fun dispose()


    abstract fun renderImage(rawImage: IImage, x: Int, y: Int, render: RenderProperties)
    abstract fun renderHandle(handle: MediumHandle, x: Int, y: Int, render: RenderProperties)


    private val transformStack = Stack<MatTrans>()
    fun pushTransform() {transformStack.push(transform)}
    fun popTransform() {transform = transformStack.pop()}

    private val stateStack = Stack<GraphicalState>()
    fun pushState() { stateStack.push( GraphicalState(transform, composite, alpha))}
    fun popState() {
        val state = stateStack.pop()
        transform = state.trans
        setComposite(state.composite, state.alpha)
    }

    open fun drawTransparencyBG(rect: Rect, i: Int) {}

}

enum class JoinMethod {
    MITER, ROUNDED, BEVEL
}

enum class CapMethod {
    NONE, ROUND, SQUARE
}

class LineAttributes (
        val width: Float,
        val cap: CapMethod,
        val join: JoinMethod,
        val dashes: FloatArray? = null
) {}

data class GraphicalState(
        val trans: MatTrans,
        val composite: GraphicsContext.Composite,
        val alpha: Float
){}
