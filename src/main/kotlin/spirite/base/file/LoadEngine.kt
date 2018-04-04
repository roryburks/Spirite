package spirite.base.file

import spirite.base.brains.IMasterControl
import spirite.base.brains.MasterControl
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.*
import spirite.hybrid.Hybrid
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.Charset

internal class LoadContext(
        val ra: RandomAccessFile,
        val workspace: MImageWorkspace)
{
    var version: Int = 0
    val chunkInfo = mutableListOf<ChunkInfo>()
}


internal data class ChunkInfo(
        val header: String,
        val startPointer: Long,
        val size: Int)

object LoadEngine {



    fun loadWorkspace( file: File, master: IMasterControl) {
        try {
            if( !file.exists())
                throw BadSifFileException("File does not exist.")

            val ra = RandomAccessFile(file, "r")
            val workspace = master.createWorkspace(1,1)
            val context = LoadContext(ra, workspace)

            // Verify Header
            ra.seek(0)
            val header = ByteArray(4).apply { ra.read(this)}

            if( ! header.contentEquals( SaveLoadUtil.header))
                throw BadSifFileException("Bad Fileheader (not an SIF File or corrupt)")

            context.version = ra.readInt()

            val width : Int
            val height: Int
            if( context.version >= 1) {
                width = ra.readShort().toInt()
                height = ra.readShort().toInt()
            }
            else {
                width = -1
                height = -1
            }

            parseChunks(context)

            // First Load the Image Data (Required)
            val imgChunk = context.chunkInfo.single { it.header == "IMGD" }
            ra.seek(imgChunk.startPointer)
            val imageMap = parseImageDataSection(context, imgChunk.size)

            // Next load the Group Data (Required), Dependent on Image Data
            val grpChunk = context.chunkInfo.single { it.header == "GRPT" }
            ra.seek(grpChunk.startPointer)

        }catch( e: IOException) {
            throw BadSifFileException("Error Reading File: " + e.getStackTrace());
        }
    }

    private fun parseChunks( context: LoadContext) {
        val buffer = ByteArray(4)

        while( context.ra.read(buffer) == 4) {
            val size = context.ra.readInt()
            val header = buffer.toString(Charset.forName("UTF-8"))
            val startPointer = context.ra.filePointer

            context.chunkInfo.add(ChunkInfo(header, startPointer, size))
            context.ra.skipBytes(size)
        }
    }

    private fun parseImageDataSection( context: LoadContext, size: Int) : Map<Int,IMedium>{
        val dataMap = mutableMapOf<Int,IMedium>()
        val ra = context.ra
        val endPointer = ra.filePointer + size

        while( ra.filePointer < endPointer) {
            val id = ra.readInt()
            val typeId = if( context.version<4) 0 else ra.readByte().toInt()

            val type = when( typeId) {
                SaveLoadUtil.MEDIUM_PLAIN -> FLAT
                SaveLoadUtil.MEDIUM_DYNAMIC -> DYNAMIC
                SaveLoadUtil.MEDIUM_PRISMATIC -> PRISMATIC
                SaveLoadUtil.MEDIUM_MAGLEV -> MAGLEV
                else -> throw BadSifFileException("Unrecognized Medium Type Id: $typeId.  Trying to load a newer SIF version in an older program version or corrupt file.")
            }

            when( type) {
                FLAT -> {
                    val imgSize = ra.readInt()
                    val img = Hybrid.imageIO.loadImage(ByteArray(imgSize).apply { ra.read(this) })

                    dataMap.put(id, FlatMedium(img, context.workspace.mediumRepository))
                }
                DYNAMIC -> {
                    val ox = ra.readShort().toInt()
                    val oy = ra.readShort().toInt()
                    val imgSize = ra.readInt()
                    val img = Hybrid.imageIO.loadImage(ByteArray(imgSize).apply { ra.read(this) })

                    dataMap.put(id, DynamicMedium(context.workspace, DynamicImage(img, ox, oy), context.workspace.mediumRepository))
                }
                PRISMATIC -> TODO()
                MAGLEV -> TODO()
                DERIVED_MAGLEV -> TODO()
            }
        }

        return dataMap
    }
}


class BadSifFileException(message: String) : Exception(message)