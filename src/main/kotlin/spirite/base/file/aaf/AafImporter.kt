package spirite.base.file.aaf

import rb.glow.img.IImage
import rbJvm.glow.awt.ImageBI
import spirite.base.imageData.MImageWorkspace
import spirite.core.hybrid.DiSet_Hybrid
import spirite.sguiHybrid.Hybrid
import java.io.File
import java.io.RandomAccessFile
import javax.imageio.ImageIO

class AafLoadContext(
        val ra: RandomAccessFile,
        val workspace: MImageWorkspace,
        val fileName: String,
        val img: IImage
)

class BadAaaFileException(message: String) : Exception(message)

object AafImporter {
    fun importIntoWorkspace(file: File, workspace: MImageWorkspace) {
        try {
            workspace.undoEngine.doAsAggregateAction("Import ${file.name}") {
                val (pngFile, aafFile) = getAafFiles(file)

                if (!aafFile.exists())
                    throw BadAaaFileException("File does not exist.")
                if (!pngFile.exists())
                    throw BadAaaFileException("Couldn't find image file for AAF")

                val image = DiSet_Hybrid.imageConverter.convertToInternal(ImageBI(ImageIO.read(pngFile)))

                RandomAccessFile(file, "r").use { ra ->
                    val versionNumber = ra.readInt()
                    AafImportFactory
                            .getImporter(versionNumber)
                            .importIntoWorkspace(AafLoadContext(ra, workspace, file.name, image))
                }
            }

        }catch( e: Exception) {
            throw BadAaaFileException("Error Reading File: ${e.message}\n${e.printStackTrace()}" )
        }
    }

    fun getAafFiles(file: File) : Pair<File, File> {
        val filename = file.absolutePath
        return when(val ext = file.extension.toLowerCase()) {
            "png" -> Pair(file, File(filename.substring(0, filename.length-3)+"aaf"))
            else -> Pair(File(filename.substring(0, filename.length - ext.length) + "png"), file)
        }
    }
}

object AafImportFactory {
    fun getImporter(version: Int) = when(version) {
        1, 3 -> AafV1Importer(version)
        else -> throw BadAaaFileException("Unrecognized version number: $version")
    }
}

interface IAafImporter {
    fun importIntoWorkspace(context: AafLoadContext)
}

