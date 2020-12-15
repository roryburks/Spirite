package rb.animo.io.aafWriter

import rb.animo.animation.AafHitbox
import rb.animo.io.FileConsts
import rb.animo.io.IWriter
import rb.vectrix.intersect.CollisionCircle
import rb.vectrix.intersect.CollisionPoint
import rb.vectrix.intersect.CollisionRigidRect
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.i
import java.io.RandomAccessFile

object HitboxWriter {
    fun write(writer: IWriter, hitbox: AafHitbox) {
        writer.writeByte(hitbox.typeId)
        when(val col = hitbox.col) {
            is CollisionPoint -> {
                writer.writeByte(FileConsts.ColKind_Point)
                writer.writeFloat(col.x.f)
                writer.writeFloat(col.y.f)
            }
            is CollisionRigidRect -> {
                writer.writeByte(FileConsts.ColKind_RigidRect)
                writer.writeFloat(col.rect.x1.f)
                writer.writeFloat(col.rect.y1.f)
                writer.writeFloat(col.rect.w.f)
                writer.writeFloat(col.rect.h.f)
            }
            is CollisionCircle -> {
                writer.writeByte(FileConsts.ColKind_Circle)
                writer.writeFloat(col.circle.x.f)
                writer.writeFloat(col.circle.y.f)
                writer.writeFloat(col.circle.r.f)
            }
        }

    }
}
