package rb.animo.io.aafReader

import rb.animo.io.FileConsts
import rb.animo.io.IReader
import rb.vectrix.intersect.*
import rb.vectrix.mathUtil.d
import rb.vectrix.shapes.CircleD
import rb.vectrix.shapes.LineSegmentD
import rb.vectrix.shapes.RectD

object HitboxReader {
    fun loadHitbox(data: IReader) : CollisionObject {
        return when( val colTypeId = data.readByte()) {
            FileConsts.ColKind_Point -> CollisionPoint(
                    data.readFloat().d,
                    data.readFloat().d
            )
            FileConsts.ColKind_RigidRect -> CollisionRigidRect(
                    RectD(
                            data.readFloat().d,
                            data.readFloat().d,
                            data.readFloat().d,
                            data.readFloat().d
                    )
            )
            FileConsts.ColKind_Circle -> CollisionCircle(
                    CircleD.Make(
                            data.readFloat().d,
                            data.readFloat().d,
                            data.readFloat().d
                    )
            )
            FileConsts.ColKind_LineSegment -> CollisionLineSegment(
                    LineSegmentD(
                            data.readFloat().d,
                            data.readFloat().d,
                            data.readFloat().d,
                            data.readFloat().d
                    )
            )
            else -> throw NotImplementedError("Unrecognized Collision Type: $colTypeId")
        }
    }
}