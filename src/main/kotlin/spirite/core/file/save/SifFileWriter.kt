package spirite.core.file.save

import rb.file.IWriteStream
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.SifFile

object SifFileWriter
{
    fun write(out: IWriteStream, file: SifFile) {
        // Header Info
        out.write(SifConstants.header) // "SIFF"
        out.writeInt(SifConstants.latestVersion)
        out.writeShort(file.width)
        out.writeShort(file.height)

        writeChunk(out, "GRPT") { SifGrptWriter.write(it, file.grptTrunk)}
        writeChunk(out, "IMGD") {SifImgdWriter.write(it, file.imgdChunk)}
    }

    fun writeChunk(out: IWriteStream, tag:String, writer: (IWriteStream)->Unit) {
        out.write(tag.toByteArray(charset("UTF-8")))

        val start = out.pointer

        // Chunklength (placeholder for now)
        out.writeInt(0)

        writer.invoke(out)

        val end = out.pointer
        out.goto(start)
        if (end - start > Integer.MAX_VALUE)
            throw SifFileException("Image Data Too Big (>2GB).")
        out.writeInt((end - start - 4).toInt())
        out.goto(end)
    }

}