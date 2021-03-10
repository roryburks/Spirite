package rb.animo.io.aaf.writer

import rb.animo.io.aaf.*
import rb.file.IFileWriter

object AafColisionWriter {
    fun write(writer: IFileWriter, col: AafFCollisionKind) {
        when( col) {
            is AafFColPoint -> {
                writer.writeByte(0) // [1] ColisionKind
                writer.writeFloat(col.x) // [4] x
                writer.writeFloat(col.y) // [4] y
            }
            is AafFColRigidRect -> {
                writer.writeByte(1) // [1] ColisionKind
                writer.writeFloat(col.x) // [4] x
                writer.writeFloat(col.y) // [4] y
                writer.writeFloat(col.w) // [4] w
                writer.writeFloat(col.h) // [4] h
            }
            is AafFColCircle -> {
                writer.writeByte(2) // [1] ColisionKind
                writer.writeFloat(col.x) // [4] x
                writer.writeFloat(col.y) // [4] y
                writer.writeFloat(col.r) // [4] r
            }
            is AafFColArc -> {
                writer.writeByte(3) // [1] ColisionKind
                writer.writeFloat(col.x) // [4] x
                writer.writeFloat(col.y) // [4] y
                writer.writeFloat(col.r) // [4] r
                writer.writeFloat(col.thStart) // [4] thSt
                writer.writeFloat(col.thEnd) // [4] thEnd
            }
            is AafFColLineSegment -> {
                writer.writeByte(4) // [1] ColisionKind
                writer.writeFloat(col.x1) // [4] x1
                writer.writeFloat(col.x1) // [4] x1
                writer.writeFloat(col.y2) // [4] y2
                writer.writeFloat(col.y2) // [4] y2
            }
            is AafFColRayRect -> {
                writer.writeByte(5) // [1] ColisionKind
                writer.writeFloat(col.x) // [4] x
                writer.writeFloat(col.y) // [4] y
                writer.writeFloat(col.h) // [4] h
                writer.writeFloat(col.len) // [4] len
                writer.writeFloat(col.theta) // [4] theta
            }
            is AafFColPoly -> {
                writer.writeByte(6) // [1] ColisionKind
                writer.writeShort(col.points.count()) // [2] : numPoints
                for (point in col.points) {
                    writer.writeFloat(point.x) // [4] x
                    writer.writeFloat(point.y) // [4] y
                }
            }
        }
    }
}