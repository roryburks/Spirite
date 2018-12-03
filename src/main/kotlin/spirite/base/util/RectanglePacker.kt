package spirite.base.util

import spirite.base.util.linear.Rect
import rb.vectrix.linear.Vec2i
import java.lang.Math.max
import java.lang.Math.round

data class PackedRectangle (
        val packedRects : List<Rect>
){
    val width: Int = packedRects.map{ it.x + it.width}.max() ?: 0
    val height: Int = packedRects.map{ it.y + it.height}.max() ?: 0
}

fun modifiedSleatorAlgorithm(  toPack : List<Vec2i>) : PackedRectangle {
    val cropped = toPack.toMutableList()

    // Remove bad Rects
    cropped.removeIf {it.xi <= 0 || it.yi <= 0}

    val minWidth = toPack.maxBy { it.xi }?.xi ?: return PackedRectangle(emptyList())

    // maxWidth and inc can be modified to effect the time spent finding
    //	the optimal Strip size (larget maxWidth and smaller inc mean more
    //	time spent, but more optimal fit)
    // inc should never be smaller than 1 and maxWidth should never be
    //	larger than the sum of all Vec2is' width
    //	(probably shouldn't be larger than the square root of the sum except
    //	in weird cases)
    val maxWidth = minWidth*2
    val inc = round( max( 1f, (maxWidth-minWidth)/10f))

    // Go through a set amount of widths to test and use the algorithm to
    //	pack the Rects, testing to see if the result is smaller in
    //	volume than the previous results.
    return (minWidth..maxWidth step inc).asSequence()
            .map { msaSub(toPack, it) }
            .minBy { it.width * it.height } ?: PackedRectangle(emptyList())
}

private fun msaSub(toPack : List<Vec2i>, width: Int) : PackedRectangle {
    // The field is essentially a 2D int array the size of the resulting
    //	strip.  Each scroll in the int array corresponds to how much free
    //	space is to the right of the position it represents (0 means the spot
    //	is currently occupied).
    // Because the height dynamically stretches whereas the width is fixed,
    //	the vertical is the position of the outer Vector whereas the horizontal
    //	is the position of the inner Array
    val field = mutableListOf<IntArray>()

    // Since we'll be doing a lot of arbitrary-index removing and the memory
    //	overhead is tiny compared to that of the field's memory consumption,
    //	LinkedList will probably be better
    val rects = toPack.toMutableList()

    // Step 0: Sort by non-increasing width
    var wy = 0
    rects.sortBy { -it.xi }

    val packed = mutableListOf<Rect>()

    // Step 1: for each Rect of width greater then half the strip width,
    //	stack them on top of each other
    rects.removeIf {
        if( it.xi >= width/2) {
            val row = IntArray(width)
            for( x in it.xi until width)
                row[x] = width-x

            field.add(row)  // Note:height guaranteed to be at least 1
            for( y in 1 until it.yi)
                field.add(row.clone())

            packed.add(Rect(0,wy,it.xi,it.yi))
            wy += it.yi
            return@removeIf true
        }
        false
    }

    // Step 2 Sort by non-increasing height for reasons
    rects.sortBy { -it.yi }

    // Step 3: go row-by-row trying to fit anything that can into the empty
    //	spaces.
    // Note: Because of our construction it's guaranteed that there are no
    //	"ceilings", i.e. Rects whose bottom is touching any air higher than
    //	yi.
    var y=0
    while( rects.any()) {
        if( field.size <= y)
            field.add(newRow(width))
        val row = field[y]

        var x = 0
        while( x < width) {
            val space = row[x]
            if( space == 0)
                ++x
            else {
                val rect = rects.firstOrNull {it.xi <= row[x]}
                if( rect != null) {
                    // Puts it (the tallest box found that can fit) at the right-most
                    //	spot to minimize weird-looking areas.
                    val newRect = Rect( x+row[x]-rect.xi, y, rect.xi, rect.yi)
                    packed.add(newRect)
                    rects.remove(rect)

                    addRect(newRect, field, width)
                    break
                }
                x += space
            }
        }
        ++y
    }

    return PackedRectangle(packed)
}

private fun newRow(width: Int) : IntArray {
    val ret = IntArray(width)
    for( i in 0 until width)
        ret[i] = width-i
    return ret
}

private fun addRect(rect:Rect, field: MutableList<IntArray>, width: Int) {
    var buildRow : IntArray? = null

    for( y in rect.y until rect.height + rect.y) {
        when(y) {
            in 0 until field.size -> {
                val row = field[y]

                for( x in 0 until rect.x)
                    row[x] = rect.x - x
                for( x in rect.x until rect.x + rect.width )
                    row[x] = 0
            }
            field.size  -> {
                if( buildRow == null) {
                    buildRow = newRow(width)

                    for( x in 0 until rect.x)
                        buildRow[x] = rect.x-x
                    for( x in rect.x until rect.width+rect.x)
                        buildRow[x] = 0
                    for( x in rect.x + rect.width until width)
                        buildRow[x] = width - x
                }
                field.add(buildRow)
            }
            else -> throw IndexOutOfBoundsException()
        }
    }
}


/** Tests to see if the PackedRectagle is poorly-described (either it has
 * intersections or has Rects that go outside of the packing bounds)
 */
fun testBad(pr: PackedRectangle): Boolean {
    for (r1 in pr.packedRects) {
        if (r1.x < 0 || r1.y < 0 || r1.x + r1.width > pr.width
                || r1.y + r1.height > pr.height) {
            return true
        }

        for (r2 in pr.packedRects) {
            if (r1 !== r2) {
                if (Rect(r1.x, r1.y, r1.width, r1.height).intersects(
                        Rect(r2.x, r2.y, r2.width, r2.height))) {
                    return true
                }
            }
        }
    }
    return false
}