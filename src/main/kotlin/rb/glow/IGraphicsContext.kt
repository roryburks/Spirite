package rb.glow

import rb.vectrix.linear.MutableTransformD

interface IGraphicsContext {
    val width: Int
    val height: Int

    val transform: MutableTransformD
    var alpha: Float
    var composite: Composite
    var color: Color
    var lineAttributes: LineAttributes

    fun pushTransform()
    fun popTransform()
    fun pushState()
    fun popState()

    fun clear(color: Color = Colors.TRANSPARENT)
}