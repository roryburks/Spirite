package spirite.core.file.save

import rb.file.IWriteStream
import rb.vectrix.mathUtil.i
import spirite.core.file.contracts.SifViewChunk

object SifViewWriter {
    const val MaxViews = 255

    fun write(out: IWriteStream, data: SifViewChunk) {
        val views = data.views.take(MaxViews)
        out.writeByte(views.size)
        for (view in views) {
            out.writeInt(view.selectedNodeId)

            // Note: Length is assumed to be the same as number of nodes in the Workspace
            for (nodeProperty in view.nodeProperties) {
                out.writeByte(nodeProperty.bitmap.i)
                out.writeFloat(nodeProperty.alpha)
                out.writeInt(nodeProperty.renderMethod)
                out.writeInt(nodeProperty.renderValue)
                out.writeShort(nodeProperty.ox)
                out.writeShort(nodeProperty.oy)
            }
        }
    }
}