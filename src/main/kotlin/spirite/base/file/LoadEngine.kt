package spirite.base.file

import spirite.base.brains.IMasterControl
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.IMedium.MediumType.*
import spirite.base.util.i
import spirite.hybrid.Hybrid

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 *
 * TODO: Wrap Java IO functionality with generic stream readin functionality
 */

internal class LoadContext(
        val ra: RandomAccessFile,
        val workspace: MImageWorkspace)
{
    var version: Int = 0
    val chunkInfo = mutableListOf<ChunkInfo>()
    val nodes = mutableSetOf<Node>()
    lateinit var reindexingMap : Map<Int,Int>

    fun reindex( index : Int) = reindexingMap[index] ?: throw BadSifFileException("Medium Id $index does not correspond to any Medium Data")
}


internal data class ChunkInfo(
        val header: String,
        val startPointer: Long,
        val size: Int)

object LoadEngine {



    fun loadWorkspace( file: File, master: IMasterControl) : MImageWorkspace{
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

            var width = 0
            var height = 0
            if( context.version >= 1) {
                width = ra.readShort().toInt()
                height = ra.readShort().toInt()
            }

            parseChunks(context)

            // Medium Data (Required)
            context.chunkInfo.single { it.header == "IMGD" }.apply {
                ra.seek(startPointer)
                parseImageDataSection(context, size)
            }

            // Group Data (Required), Dependent on Image Data
            context.chunkInfo.single { it.header == "GRPT" }.apply {
                ra.seek(startPointer)
                parseGroupTreeSection(context, size)
            }

            // Animation Data (optional), dependent on Group Data and Image Data
            context.chunkInfo.singleOrNull { it.header == "ANIM" }?.apply {
                ra.seek(startPointer)
                parseAnimationData(context, size)
            }

            // Palette Data (optional)
            context.chunkInfo.singleOrNull { it.header == "PLTT" }?.apply {
                ra.seek(startPointer)
                parsePaletteData(context,size)
            }

            if( context.version <= 2) {
                width = workspace.mediumRepository.dataList
                        .map { workspace.mediumRepository.getData(it)?.width ?: 0}
                        .max() ?: 100
                height = workspace.mediumRepository.dataList
                        .map { workspace.mediumRepository.getData(it)?.height ?: 0}
                        .max() ?: 100
            }

            workspace.width = width
            workspace.height = height

            workspace.finishBuilding()

            return workspace
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

    private fun parseImageDataSection( context: LoadContext, size: Int) {
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
                    val imgData = ByteArray(imgSize).apply { ra.read( this) }

                    File("C:/Bucket/.dump").writeBytes(imgData)

                    val img = Hybrid.imageIO.loadImage(imgData)
                    dataMap[id] = FlatMedium(img, context.workspace.mediumRepository)
                }
                DYNAMIC -> {
                    val ox = ra.readShort().i
                    val oy = ra.readShort().i
                    val imgSize = ra.readInt()
                    val imgData = ByteArray(imgSize).apply { ra.read( this) }

                    val img = Hybrid.imageIO.loadImage(imgData)
                    dataMap[id] = DynamicMedium(context.workspace, DynamicImage(img, ox, oy), context.workspace.mediumRepository)
                }
                PRISMATIC -> TODO()
                MAGLEV -> TODO()
                DERIVED_MAGLEV -> TODO()
            }
        }

        context.reindexingMap = context.workspace.mediumRepository.importMap(dataMap)
    }

    private fun parseGroupTreeSection(context: LoadContext, chunkSize: Int) {
        if( context.version <= 1) TODO()

        val ra = context.ra
        val workspace = context.workspace
        val endPointer = ra.filePointer + chunkSize

        // Create an array that keeps track of the active layers of group nodes
        //  (all the nested nodes leading up to the current node)
        val nodeLayer = Array<GroupNode?>(256, {null})

        nodeLayer[0] = workspace.groupTree.root
        context.nodes.add(workspace.groupTree.root)
        val referencesToMap = mutableMapOf<LayerNode, Int>()

        while( ra.filePointer < endPointer) {
            var alpha = 1.0f
            var x = 0
            var y = 0
            var bitmask = 0x1 + 0x2

            val depth = ra.readUnsignedByte()

            if( context.version >= 1) {
                alpha = ra.readFloat()
                x = ra.readShort().i
                y = ra.readShort().i
                bitmask = ra.readUnsignedByte()
            }

            val name = SaveLoadUtil.readNullTerminatedStringUTF9(ra)
            val type = ra.readUnsignedByte()


            // !!!! Kind of hack-y that it's even saved, but only the root node should be
            //	depth 0 and there should only be one (and it's already created)
            if( depth == 0)
                continue

            val node = when( type) {
                SaveLoadUtil.NODE_GROUP -> {
                    workspace.groupTree.addGroupNode(nodeLayer[depth - 1], name)
                            .apply { nodeLayer[depth] = this }
                }
                SaveLoadUtil.NODE_SIMPLE_LAYER -> {
                    val mediumId = ra.readInt()

                    val layer = SimpleLayer(MediumHandle(workspace, context.reindex(mediumId)))
                    workspace.groupTree.importLayer( nodeLayer[depth-1], name, layer, true)
                }
                SaveLoadUtil.NODE_RIG_LAYER -> TODO()
                SaveLoadUtil.NODE_REFERENCE_LAYER -> TODO()
                SaveLoadUtil.NODE_PUPPET_LAYER -> TODO()
                else -> throw BadSifFileException("Unrecognized Node Type ID: $type (version mismatch or corrupt file?)")
            }

            context.nodes.add(node)
            node.alpha = alpha
            node.expanded = bitmask and SaveLoadUtil.EXPANDED_MASK != 0
            node.visible = bitmask and SaveLoadUtil.VISIBLE_MASK != 0
            node.x = x
            node.y = y
        }


        // Link the reference nodes (needs to be done afterwards because it might link to a node yet
        //	added to the node map since nodeIDs are based on depth-first Group Tree order,
        //	not creation order) (TODO once ReferenceLayers are in)
    }

    private fun parseAnimationData( context: LoadContext, chunkSize: Int)
    {
        //TODO()
    }

    private fun parsePaletteData( context: LoadContext, chunkSize: Int)
    {
        //TODO()
    }
}


class BadSifFileException(message: String) : Exception(message)