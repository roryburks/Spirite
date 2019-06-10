package rb.vectrix.rectanglePacking

import rb.extendo.extensions.removeToList
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.floor
import rb.vectrix.shapes.RectI
import kotlin.math.max
import kotlin.math.sqrt

object LeftRightPacker  : IRectanglePackingAlgorithm {
    override fun pack(toPack: List<Vec2i>): PackedRectangle {
        val cropped = toPack.filter { it.xi > 0 && it.yi > 0 }.sortedBy { -it.xi }
        val minWidth = sqrt(cropped.sumBy { it.xi * it.yi }.d).floor * 3 / 4

        val maxWidth = minWidth*2
        val inc = max(1, (maxWidth-minWidth + 5)/10)

        return (minWidth..maxWidth step inc).asSequence()
            .map { packSub(cropped, it) }
            .minBy { it.width * it.height } ?: NilPacked
    }

    private fun packSub(rectsToPack: List<Vec2i>, width: Int) : PackedRectangle {
        val unpacked = rectsToPack.toMutableList()
        val rows = mutableListOf<Row>()
        val packed = mutableListOf<RectI>()

        // Step 1: Add all pieces > width/2, widest first
        var wy = 0
        unpacked
            .removeToList { it.xi >= width/2 }
            .forEach { dim ->
                packed.add(RectI(0, wy, dim.xi, dim.yi))
                rows.add(Row(dim.yi, width - dim.xi))
                wy += dim.yi
            }

        unpacked.sortBy { -it.yi }

        wy = 0
        while (unpacked.any())
        {
            val row = rows.firstOrNull()
            if( row == null) {
                val toPack = unpacked.removeAt(0)
                packed.add(RectI(0, wy, toPack.xi, toPack.yi))
                rows.add(Row(toPack.yi, width - toPack.xi))
            }
            else {
                val toPack = unpacked.firstOrNull { it.xi <= row.free_w }
                if (toPack == null) {
                    wy += row.h
                    rows.removeAt(0)
                } else {
                    unpacked.remove(toPack)
                    packed.add(RectI(width - row.right - toPack.xi, wy, toPack.xi, toPack.yi))

                    when {
                        toPack.yi == row.h -> row.right -= toPack.xi
                        toPack.yi <= row.h -> {
                            rows.add(1, Row(row.h - toPack.yi, row.free_w, row.right))
                            row.h = toPack.yi
                            row.right += toPack.xi
                            row.free_w -= toPack.xi
                        }
                        else -> {
                            val right = toPack.xi + row.right
                            var row_i = 0
                            var height_to_consume = toPack.yi
                            while (height_to_consume > 0) {
                                val rowToModify = rows.getOrNull(row_i)
                                if( rowToModify == null){
                                    rows.add(Row(height_to_consume,width - right, right))
                                    break
                                }
                                else if( rowToModify.h > height_to_consume){
                                    rows.add(row_i+1, Row(rowToModify.h - height_to_consume, rowToModify.free_w, rowToModify.right))
                                    rowToModify.h = height_to_consume
                                    rowToModify.free_w -= right - rowToModify.right
                                    rowToModify.right = right
                                    break
                                }
                                else {
                                    height_to_consume -= rowToModify.h
                                    rowToModify.free_w -= right - rowToModify.right
                                    rowToModify.right = right
                                    row_i++
                                }
                            }
                        }
                    }
                }
            }
        }

        return PackedRectangle(packed)
    }

    private class Row(
        var h: Int,
        var free_w: Int,
        var right: Int = 0)
}