package rb.vectrix.rectanglePacking

import rb.extendo.extensions.removeFirst
import rb.extendo.extensions.removeToList
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.floor
import rb.vectrix.shapes.RectI
import kotlin.math.max
import kotlin.math.sqrt

object BottomUpPacker  : IRectanglePackingAlgorithm {
    override fun pack(toPack: List<Vec2i>): PackedRectangle {
        val cropped = toPack.filter { it.xi > 0 && it.yi > 0 }.sortedBy { -it.xi }
        val minWidth = sqrt(cropped.sumBy { it.xi * it.yi }.d).floor * 3 / 4

        val maxWidth = minWidth*10
        val inc = max(1, (maxWidth-minWidth + 5)/10)

        return (minWidth..maxWidth step inc).asSequence()
            .map { packSub(cropped, it) }
            .minBy { it.width * it.height } ?: NilPacked
    }

    private fun packSub(rectsToPack: List<Vec2i>, spaceWidth: Int) : PackedRectangle {
        val unpacked = rectsToPack.toMutableList()
        val rows = mutableListOf<Row>()
        val packed = mutableListOf<RectI>()

        // Step 1: Add all pieces > spaceWidth/2, widest first
        var wy = 0
        unpacked
            .removeToList { it.xi >= spaceWidth/2 }
            .forEach { dim ->
                packed.add(RectI(0, wy, dim.xi, dim.yi))
                rows.add(BottomUpPacker.Row.Left(dim.yi, dim.xi, spaceWidth))
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
                rows.add(BottomUpPacker.Row.Left(toPack.yi, toPack.xi, spaceWidth))
            }
            else {
                val set = unpacked.asSequence()
                    .flatMap { x -> row.freeWidths.asSequence().map { Pair(x, it) } }
                    .firstOrNull { it.first.xi <= it.second.last - it.second.first }
                if (set == null) {
                    wy += row.h
                    rows.removeAt(0)
                } else {
                    val (toPack, range) = set
                    unpacked.remove(toPack)
                    val left = range.last - toPack.xi
                    packed.add(RectI(left, wy, toPack.xi, toPack.yi))

                    InsertRect(rows, toPack, left, spaceWidth)
                }
            }
        }

        return PackedRectangle(packed)
    }

    private fun InsertRect(rows: MutableList<Row>, toInsert:Vec2i, left: Int, spaceWidth: Int) {
        val first = rows.first()
        when {
            first.h == toInsert.yi -> first.insert(left, toInsert.xi)
            first.h < toInsert.yi -> {
                rows.add(1, Row(first.h - toInsert.yi, first.freeWidths.toMutableList()))
                first.h = toInsert.yi
                first.insert(left, toInsert.xi)
            }
            else -> {
                var row_i = 0
                var height_to_consume = toInsert.yi
                while (height_to_consume > 0) {
                    val rowToModify = rows.getOrNull(row_i)
                    if( rowToModify == null) {
                        rows.add(BottomUpPacker.Row.Middle(height_to_consume, left, toInsert.xi, spaceWidth))
                        break
                    }
                    else if( rowToModify.h > height_to_consume)  {
                        rows.add(row_i+1, Row(rowToModify.h - height_to_consume, rowToModify.freeWidths.toMutableList()))
                        rowToModify.insert(left, toInsert.xi)
                        break
                    }
                    else {
                        height_to_consume -= rowToModify.h
                        rowToModify.insert(left, toInsert.xi)
                        ++row_i
                    }
                }
            }
        }

    }

    private class Row(
        var h: Int,
        var freeWidths: MutableList<IntRange>)
    {
        fun insert( left: Int, width: Int) {
            val toModify = freeWidths.removeFirst { it.contains(left) } ?: return
            if( toModify.first < left) freeWidths.add(toModify.first..left)
            if( left + width < toModify.last) freeWidths.add((left + width)..toModify.last)
        }

        companion object {
            fun Left(h: Int, width: Int, spaceWidth: Int) = Row(h, mutableListOf(width..spaceWidth))
            fun Middle( h: Int, left: Int, width: Int, spaceWidth: Int) : Row {
                if( left == 0) return Left(h, width, spaceWidth)
                if( left + width == spaceWidth) return Row( h, mutableListOf(0..left))
                return Row(h, mutableListOf(0..left, (left+width)..spaceWidth  ))
            }
        }

    }


}