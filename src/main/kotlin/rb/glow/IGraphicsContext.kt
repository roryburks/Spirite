package rb.glow

import rb.extendo.dataStructures.Deque
import rb.glow.gle.RenderRubric
import rb.glow.img.IImage
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.MutableTransformD
import rb.vectrix.shapes.Rect

interface IGraphicsContext {

    val width: Int
    val height: Int

    var transform: ITransform
    var alpha: Float
    var composite: Composite
    var color: Color
    var lineAttributes : LineAttributes

    fun clear( color: Color = Colors.TRANSPARENT)

    // Transform Mutations
    fun preTranslate(offsetX: Double, offsetY: Double)
    fun translate(offsetX: Double, offsetY: Double)
    fun preTransform(trans: ITransform)
    fun transform(trans: ITransform)
    fun preScale(sx: Double, sy: Double)
    fun scale(sx: Double, sy: Double)

    // Draw
    fun drawPolyLine( x: Iterable<Double>, y: Iterable<Double>, count: Int, loop: Boolean = false)
    fun fillPolygon(x: Iterable<Double>, y: Iterable<Double>, count: Int)

    // Rendering
    fun renderImage(image: IImage, x: Double, y: Double, renderRubric: RenderRubric? = null, imgPart: Rect? = null)

    fun setClip( i: Int, j: Int, width: Int, height: Int)

    fun pushTransform()
    fun popTransform()
    fun pushState()
    fun popState()
}

// A pseudo-implementation of IGraphicsContext which handles a lot of the ♂
abstract class AGraphicsContext : IGraphicsContext {
    private var _transform: MutableTransformD = MutableTransformD.Identity
    override var transform: ITransform
        get() = _transform
        set(value) {_transform = MutableTransformD(value.m00, value.m01, value.m02, value.m10, value.m11, value.m12) }

    // region Transform
    private val transformStack = Deque<ITransform>()

    override fun pushTransform() {transformStack.addBack(transform.toMutable())}
    override fun popTransform() {transform = transformStack.popBack() ?: ITransform.Identity}

    override fun preTranslate(offsetX: Double, offsetY: Double) { _transform.preTranslate(offsetX, offsetY)}
    override fun translate(offsetX: Double, offsetY: Double) { _transform.translate(offsetX, offsetY)}
    override fun preTransform(trans: ITransform) { _transform.preConcatenate(trans) }
    override fun transform(trans: ITransform) { _transform.concatenate(trans)}
    override fun preScale(sx: Double, sy: Double) {_transform.preScale(sx, sy)}
    override fun scale(sx: Double, sy: Double) {_transform.scale(sx, sy)}
    // endregion

    // region StateStack
    private data class GcState(
            val trans: ITransform,
            val composite: Composite,
            val alpha: Float,
            val color: Color )

    private val stateStack = Deque<GcState>()

    override fun pushState() { stateStack.addBack(GcState(transform.toMutable(), composite, alpha, color)) }
    override fun popState() {
        val state = stateStack.popBack() ?: return
        transform = state.trans
        alpha = state.alpha
        composite = state.composite
        color = state.color
    }
    // endregion
}

enum class JoinMethod {MITER, ROUNDED, BEVEL}

enum class CapMethod {NONE, ROUND, SQUARE}


enum class Composite {
    SRC, SRC_IN, SRC_OVER, SRC_OUT, SRC_ATOP,
    DST, DST_IN, DST_OVER, DST_OUT, DST_ATOP,
    CLEAR, XOR,
    ADD
}

class LineAttributes (
        val width: Float,
        val cap: CapMethod = CapMethod.NONE,
        val join: JoinMethod = JoinMethod.MITER,
        val dashes: FloatArray? = null)

data class GraphicalState(
        val trans: ITransform,
        val composite: Composite,
        val alpha: Float,
        val color: Color)
