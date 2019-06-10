package rb.vectrix.rectanglePacking

import rb.extendo.extensions.removeFirst
import rb.extendo.extensions.removeToList
import rb.vectrix.linear.Vec2i
import rb.vectrix.shapes.RectI
import kotlin.math.max
import kotlin.math.min


fun ModifiedSleatorAlgorithm(toPack: List<Vec2i>) : PackedRectangle {
    val cropped = toPack.filter { it.xi > 0 && it.yi > 0 }.sortedBy { -it.xi }
    val minWidth = toPack.asSequence().map { it.xi }.max() ?: return NilPacked

    val maxWidth = minWidth*2
    val inc = max(1, (maxWidth-minWidth + 5)/10)

    return (minWidth..maxWidth step inc).asSequence()
        .map { msaSub(cropped, it) }
        .minBy { it.width * it.height } ?: NilPacked
}
private fun msaSub(toPack: List<Vec2i>, width: Int) : PackedRectangle {
    val field = mutableListOf<IntArray>()
    val unpacked = toPack.toMutableList()
    val packed = mutableListOf<RectI>()

    var wy = 0
    unpacked.removeToList { it.xi >= width/2 }
        .forEach {dim ->
            val row = emptyRow(width)
            repeat(dim.xi) { row[it] = 0}

            field.add(row)
            repeat(dim.yi-1) {field.add(row.copyOf())}

            packed.add(RectI(0,wy, dim.xi, dim.yi))
            wy += dim.yi
        }

    unpacked.sortBy { -it.yi }

    // Step 3: go row-by-row trying to fit anything that can into the empty
    //	spaces.
    // Note: Because of our construction it's guaranteed that there are no
    //	"ceilings", i.e. Rects whose bottom is touching any air higher than
    //	yi.
    fun addRect( rect: RectI) {
        for (y in rect.y1i until rect.y2i) {
            val row = field.getOrNull(y) ?: emptyRow(width).also { field.add(it) }
            (0 until rect.x1i).forEach { row[it] = min(row[it], rect.x1i - it) }
            (rect.x1i until rect.x2i).forEach { row[it] = 0 }
        }
    }

    var y = 0
    while (unpacked.any()) {
        while( field.size <= y) field.add(emptyRow(width))
        val row = field[y]

        var x = 0
        while (x < width) {
            val space = row[x]
            if( space == 0)
                x++
            else {
                val dim = unpacked.removeFirst { it.xi < space } ?: break
                val rect = RectI(x, y, dim.xi, dim.yi)
                packed.add(rect)
                addRect(rect)
                x += dim.xi
            }
        }
        ++y
    }

    return PackedRectangle(packed)
}

fun emptyRow(w: Int) = IntArray(w) {w - it}