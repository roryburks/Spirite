package rb.animo.io.aaf.reader

import rb.animo.io.aaf.*
import rb.file.IReadStream

object AafColisionReader {
    fun read( reader: IReadStream) : AafFCollisionKind {
        return when(val type = reader.readUnsignedByte()) {
            0 -> AafFColPoint(
                x = reader.readFloat(),
                y = reader.readFloat())
            1 -> AafFColRigidRect(
                x = reader.readFloat(),
                y = reader.readFloat(),
                w = reader.readFloat(),
                h = reader.readFloat() )
            2 -> AafFColCircle(
                x = reader.readFloat(),
                y=reader.readFloat(),
                r=reader.readFloat() )
            3 -> AafFColArc(
                x = reader.readFloat(),
                y = reader.readFloat(),
                r = reader.readFloat(),
                thStart =  reader.readFloat(),
                thEnd = reader.readFloat() )
            4 -> AafFColLineSegment(
                x1 = reader.readFloat(),
                y1 = reader.readFloat(),
                x2 = reader.readFloat(),
                y2 = reader.readFloat() )
            5 -> AafFColRayRect(
                x = reader.readFloat(),
                y = reader.readFloat(),
                h = reader.readFloat(),
                len = reader.readFloat(),
                theta = reader.readFloat() )
            6 -> {
                val numPoints = reader.readUnsignedShort()
                AafFColPoly(List(numPoints){
                    AafFColPoint(
                        x = reader.readFloat(),
                        y = reader.readFloat()
                    )
                })
            }
            else -> throw NotImplementedError("Unrecognized Colision Type: $type")
        }
    }

}