package spirite.base.graphics

import rb.glow.color.Color
import rb.vectrix.linear.ITransform

interface IGraphicsContext {
    val width: Int
    val height: Int

    var transform: ITransform
    var alpha: Float
    var composite: Composite
    var color: Color
}