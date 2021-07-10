package spirite.base.file.sif.v2

import rb.file.BigEndianWriteStream
import rbJvm.file.writing.JvmRaRawWriteStream
import sgui.core.systems.IImageIO
import spirite.base.file.sif.v2.converters.SifWorkspaceExporter
import spirite.base.imageData.IImageWorkspace
import spirite.core.file.save.SifFileWriter
import spirite.sguiHybrid.Hybrid
import java.io.File
import java.io.RandomAccessFile

object JvmSpiriteSaveLoad {
    val _imageIo : IImageIO = Hybrid.imageIO

    fun write(file : File, workspace: IImageWorkspace) {
        val converted = SifWorkspaceExporter(_imageIo).export(workspace)

        if( file.exists())
        {
            val backup = File(file.absolutePath+"~")
            if( backup.exists())
                backup.delete()
            val canWrite = file.canWrite()
            val x = file.renameTo(backup)
            val deleted = file.delete()
        }

        file.createNewFile()

        val ra = RandomAccessFile(file, "rw")
        val raw = JvmRaRawWriteStream(ra)
        //val buffered = BufferedWriteStream(raw)
        val out = BigEndianWriteStream(raw)
        SifFileWriter.write(out, converted)
        ra.close()
    }
}