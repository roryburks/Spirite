package rb.vectrix.rectanglePacking

import rb.vectrix.linear.Vec2i
import rb.vectrix.shapes.RectI

data class PackedRectangle (
    val packedRects : List<RectI>
){
    val width: Int = packedRects.map{ it.x1i + it.wi}.max() ?: 0
    val height: Int = packedRects.map{ it.y1i + it.hi}.max() ?: 0
}

val NilPacked = PackedRectangle(emptyList())


interface IRectanglePackingAlgorithm {
    fun pack(toPack: List<Vec2i>) : PackedRectangle
}