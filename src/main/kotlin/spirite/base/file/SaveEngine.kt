package spirite.base.file

import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType
import spirite.hybrid.MDebug.WarningType.STRUCTURAL
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

class SaveContext(
        val workspace: IImageWorkspace,
        val ra: RandomAccessFile)
{
    val nodeMap = mutableMapOf<Node, Int>()
    val root = workspace.groupTree.root
    val floatingData = workspace.mediumRepository.dataList
            .map { workspace.mediumRepository.floatData(it, {MediumPreparer.prepare(it)}) }
            .filterNotNull()


    fun writeChunk( tag: String, writer : (RandomAccessFile)->Unit) {
        if( tag.length != 4) {
            // Perhaps overkill, but this really should be a hard truth
            MDebug.handleError(ErrorType.FATAL, "Chunk types must be 4-length")
        }

        // [4] : Chunk Tag
        ra.write( tag.toByteArray(charset("UTF-8")))

        val start = ra.filePointer
        // [4] : ChunkLength (placeholder for now
        ra.writeInt(0)

        writer.invoke(ra)

        val end = ra.filePointer
        ra.seek(start)
        if( end - start > Integer.MAX_VALUE)
            MDebug.handleError(ErrorType.OUT_OF_BOUNDS, "Image Data Too Big (>2GB).")
        ra.writeInt((end - start - 4).toInt())
        ra.seek(end)
    }
}

object SaveEngine {

    fun saveWorkspace( file : File, workspace: IImageWorkspace) {
        val overwrite = file.exists()
        val saveFile = if(overwrite) File(file.absolutePath + "~") else file

        if( saveFile.exists())
            saveFile.delete()
        saveFile.createNewFile()

        val ra = RandomAccessFile(file, "rw")
        val context = SaveContext(workspace, ra)

        saveHeader(context)
        saveGroupTree(context)
        saveImageData(context)
        //saveAnimationData(context)
        //savePaletteData(context)

        ra.close()
    }

    fun saveHeader( context: SaveContext) {
        val ra = context.ra
        val workspace = context.workspace

        // [4] Header
        ra.write( SaveLoadUtil.header)
        // [4] Version
        ra.writeInt(SaveLoadUtil.version)

        // [2] Width, [2] Height
        ra.writeShort(workspace.width)
        ra.writeShort(workspace.height)
    }

    /** GRPT chunk saving the structure of the PrimaryGroupTree and all Nodes/Layers within it */
    fun saveGroupTree( context: SaveContext)
    {
        context.writeChunk("GRPT", {ra->
            val depth = 0
            var met = 0

            fun buildReferences( node: Node, depth: Int) {
                // Fills out a map from nodes to an int identifier (constructed here) for use in any nodes that
                //  reference other nodes (such as ReferenceLayers)
                context.nodeMap.put( node, met++)
                if( node is GroupNode)
                    node.children.forEach { buildReferences(it, depth+1) }
            }
            fun writeNode( node: Node, depth: Int) {
                // [1] : Depth of Node in GroupTree
                ra.writeByte(depth)

                // [4] : alpha
                ra.writeFloat( node.alpha)

                // [2] : x offset, [2] : y offset
                ra.writeShort(node.x)
                ra.writeShort(node.y)

                // [1] : bitmask
                ra.writeByte(
                        if( node.visible) SaveLoadUtil.VISIBLE_MASK else 0 +
                        if( node.expanded) SaveLoadUtil.EXPANDED_MASK else 0 )

                // [n], UTF8 : Layer Name
                ra.write(SaveLoadUtil.strToByteArrayUTF8(node.name))

                when( node) {
                    is GroupNode -> {
                        // [1] : NodeTypeId
                        ra.writeByte( SaveLoadUtil.NODE_GROUP)

                        // Go through each of the Group Node's children recursively and save them
                        when( depth) {
                            0xFF -> if( node.children.any()) MDebug.handleWarning(STRUCTURAL, "Too many nested groups (255 limit), some nodes ignored.")
                            else -> node.children.forEach { writeNode(it, depth+1) }
                        }
                    }
                    is LayerNode -> {
                        val layer = node.layer
                        when( layer) {
                            is SimpleLayer -> {
                                // [1] : NodeTypeId
                                ra.writeByte( SaveLoadUtil.NODE_SIMPLE_LAYER)

                                // [4] : MediumId of Medium attatched to the SimpleLayer
                                ra.writeInt( layer.medium.id)
                            }
                            is SpriteLayer -> {
                                TODO()
                            }
                            //is ReferenceLayer -> {}
                            //is PuppetLayer -> {}
                        }
                    }
                }

            }

            buildReferences(context.workspace.groupTree.root, depth)
            writeNode(context.root, 0)
        })
    }

    /** PLTT chunk containing the Palettes saved in the workspace */
    fun savePaletteData( context: SaveContext) {
        context.writeChunk("PLTT", {ra ->
            context.workspace.paletteSet.palettes.forEach {
                // [n], UTF8 : Palette Name
                ra.write( SaveLoadUtil.strToByteArrayUTF8(it.name))

                val raw = it.compress()
                ra.writeShort(raw.size) // [2] Palette Data Size
                ra.write(raw)           // [n] Compressed Palette Data
            }
        })
    }

    /** IMGD chunk containing all Medium Data, with images saved in PNG Format*/
    fun saveImageData( context: SaveContext) {
        context.writeChunk("IMGD", {ra->
            context.floatingData.forEach {
                // [4] : Medium Handle Id
                ra.write( it.id)

                val prepared = it.condensed
                when( prepared) {
                    is PreparedFlatMedium -> {
                        ra.writeByte(SaveLoadUtil.MEDIUM_PLAIN) // [1] : Medium Type

                        val byteArray = Hybrid.imageSaver.writePNG(prepared.image)
                        ra.writeInt( byteArray.size)    // [4] : Size of Image Data
                        ra.write(byteArray)             // [n] : Image Data
                    }
                    is PreparedDynamicMedium -> {
                        ra.writeByte(SaveLoadUtil.MEDIUM_PLAIN) // [1] : Medium Type
                        ra.writeShort( prepared.offsetX)        // [2] : Dynamic X Offset
                        ra.writeShort( prepared.offsetY)        // [2] : Dynamic Y Offset

                        val byteArray = prepared.image?.run { Hybrid.imageSaver.writePNG(this)}
                        ra.writeInt( byteArray?.size ?: 0)  // [4] : Size of Image Data (note: May be 0)
                        byteArray?.run { ra.write(this)}    // [n] : Image Data
                    }
                }
            }
        })

    }
}