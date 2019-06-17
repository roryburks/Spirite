package rb.glow

import rb.glow.color.Color
import rb.glow.color.Colors
import rb.vectrix.linear.MutableTransformD
import spirite.base.graphics.Composite
import spirite.base.graphics.LineAttributes

interface IGraphicsContext {
    val width: Int
    val height: Int

    val transform: MutableTransformD
    var alpha: Float
    var composite: Composite
    var color: Color
    var lineAttributes: LineAttributes

    fun clear(color: Color = Colors.TRANSPARENT)
}