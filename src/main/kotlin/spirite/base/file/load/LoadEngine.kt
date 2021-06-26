package spirite.base.file.load

import rb.glow.Color
import rb.glow.ColorARGB32Normal
import rb.vectrix.mathUtil.d
import rb.vectrix.mathUtil.i
import spirite.base.brains.IMasterControl
import spirite.base.file.SaveLoadUtil
import spirite.base.file.readUTF8NT
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpritePartStructure
import spirite.base.imageData.mediums.IMedium
import spirite.base.telemetry.TelemetryEvent
import spirite.base.telemetry.TelemetryStopwatch
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 *
 * TODO: Wrap Java IO functionality with generic stream readin functionality
 */

class LoadContext(
        val ra: RandomAccessFile,
        val workspace: MImageWorkspace)
{
    var version: Int = 0
    val chunkInfo = mutableListOf<ChunkInfo>()
    val nodes = mutableListOf<Node>()
    val animations = mutableListOf<Animation>()
    val telemetry = TelemetryEvent()
    val tel2 = TelemetryStopwatch()

    lateinit var reindexingMap : Map<Int,Int>

    fun reindex( index : Int) = reindexingMap[index] ?: throw BadSifFileException("Medium Id $index does not correspond to any Medium Data")
}


data class ChunkInfo(
        val header: String,
        val startPointer: Long,
        val size: Int)

object LoadEngine {



    fun loadWorkspace( file: File, master: IMasterControl) : MImageWorkspace{
        try {
            if (!file.exists())
                throw BadSifFileException("File does not exist.")

            val ra = RandomAccessFile(file, "r")

            try {
                val workspace = master.createWorkspace(1, 1)
                val paletteDriving = workspace.paletteManager.drivePalette
                workspace.paletteManager.drivePalette = false
                val context = LoadContext(ra, workspace)

                // Verify Header
                ra.seek(0)
                val header = ByteArray(4).apply { ra.read(this) }

                if (!header.contentEquals(SaveLoadUtil.header))
                    throw BadSifFileException("Bad Fileheader (not an SIF File or corrupt)")

                context.version = ra.readInt()

                var width = 0
                var height = 0
                if (context.version >= 1) {
                    width = ra.readShort().toInt()
                    height = ra.readShort().toInt()
                }

                parseChunks(context)

                // Medium Data (Required)
                context.chunkInfo.single { it.header == "IMGD" }.apply {
                    context.telemetry.runAction("Load IMGD") {
                        ra.seek(startPointer)
                        parseImageDataSection(context, size)
                        context.telemetry.mark("size", size.d)
                    }
                }

                // Group Data (Required), Dependent on Image Data
                context.chunkInfo.single { it.header == "GRPT" }.apply {
                    context.telemetry.runAction("Load GRPT") {
                        ra.seek(startPointer)
                        parseGroupTreeSection(context, size)
                        context.telemetry.mark("size", size.d)
                    }
                }

                // Animation Data (optional), dependent on Group Data and Image Data
                context.chunkInfo.singleOrNull { it.header == "ANIM" }?.apply {
                    context.telemetry.runAction("Load ANIM") {
                        ra.seek(startPointer)
                        parseAnimationData(context, size)
                        context.telemetry.mark("size", size.d)
                    }
                }
                // Animation Space Data (optional)
                context.chunkInfo.singleOrNull { it.header == "ANSP" }?.apply {
                    context.telemetry.runAction("Load ANSP") {
                        ra.seek(startPointer)
                        parseAnimationSpaceData(context, size)
                        context.telemetry.mark("size", size.d)
                    }
                }

                // Palette Data (optional)
                context.chunkInfo.singleOrNull { it.header == "PLTT" }?.apply {
                    context.telemetry.runAction("Load PLTT") {
                        ra.seek(startPointer)
                        parsePaletteData(context, size)
                        context.telemetry.mark("size", size.d)
                    }
                }

                // Palette Map Data (optional)
                context.chunkInfo.singleOrNull { it.header == "TPLT" }?.apply {
                    context.telemetry.runAction("Load TPLT") {
                        ra.seek(startPointer)
                        parsePaletteMapData(context, size)
                        context.telemetry.mark("size", size.d)
                    }
                }

                // View Data (optional)
                context.chunkInfo.singleOrNull { it.header == "VIEW" }?.apply {
                    context.telemetry.runAction("Load VIEW") {
                        ra.seek(startPointer)
                        ViewChunkLoader.load(context)
                        context.telemetry.mark("size", size.d)
                    }
                }


                if (context.version <= 2) {
                    width = workspace.mediumRepository.dataList
                        .map { workspace.mediumRepository.getData(it)?.width ?: 0 }
                        .max() ?: 100
                    height = workspace.mediumRepository.dataList
                        .map { workspace.mediumRepository.getData(it)?.height ?: 0 }
                        .max() ?: 100
                }

                workspace.width = width
                workspace.height = height

                print(context.telemetry)

                workspace.finishBuilding()

                //adHocSave(workspace)
                workspace.mediumRepository.clearUnusedCache(emptySet())

                workspace.paletteManager.drivePalette = paletteDriving


                return workspace
            } finally {
                ra.close()
            }
        }finally {

        }
//        }catch( e: Exception) {
//            throw
//            //throw BadSifFileException("Error Reading File: ${e.message}\n${e.printStackTrace()}" )
//        }
    }

    val adHocRecover = listOf(13,27,41,59,74,89,104,119,134,150,166,182,198,214,229,244,259,276,291,306,322,337,352)
    fun adHocSave(workspace: MImageWorkspace) {

        val unused = workspace.mediumRepository.getUnused(emptySet())
        val group = workspace.groupTree.addGroupNode(null, "SAVED")
        var m = 0

        val groupedMedHandles = mutableListOf<List<MediumHandle>>()
        var workingGroup = mutableListOf<MediumHandle>()
        unused.mapIndexed{ i, mh ->
            workingGroup.add(mh)
            if( adHocRecover.contains(i)) {
                groupedMedHandles.add(workingGroup)
                workingGroup = mutableListOf()
            }
        }

        groupedMedHandles
                .map { SpriteLayer(workspace, it.mapIndexed {  i, h -> Pair(h, SpritePartStructure(i, "$i")) }) }
                .forEachIndexed { index, spriteLayer -> workspace.groupTree.importLayer(group, "$index", spriteLayer) }
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

    private fun parseImageDataSection(context: LoadContext, chunkSize: Int) {
        val dataMap = mutableMapOf<Int,IMedium>()
        val ra = context.ra
        val endPointer = ra.filePointer + chunkSize

        while( ra.filePointer < endPointer) {
            //if( ra.filePointer > 340000)
              //  println("A")
            val id = ra.readInt()
            //if( id == 38)
              //  continue
            val typeId = if( context.version<4) 0 else ra.readByte().toInt()

            val medium = MediumLoaderFactory
                    .getMediumLoader(context.version, typeId)
                    .loadMedium(context)

            if( medium != null)
                dataMap[id] = medium
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
        val nodeLayer = Array<GroupNode?>(256) {null}

        nodeLayer[0] = workspace.groupTree.root
        context.nodes.add(workspace.groupTree.root)

        while( ra.filePointer < endPointer) {
            var alpha = 1.0f
            var x = 0
            var y = 0
            var bitmask = 0x1 + 0x2

            val depth = ra.readUnsignedByte()

            if( context.version >= 1) {
                if( context.version < 0x0001_0010) {
                    alpha = ra.readFloat()
                    x = ra.readShort().i
                    y = ra.readShort().i
                }
                bitmask = ra.readUnsignedByte()
            }

            val expanded = bitmask and SaveLoadUtil.ExpandedMask != 0
            val visble = bitmask and SaveLoadUtil.VisibleMask != 0
            val flatenned = bitmask and SaveLoadUtil.FlattenedMask != 0

            val name = SaveLoadUtil.readNullTerminatedStringUTF8(ra)
            val type =  ra.readUnsignedByte()

            // !!!! Kind of hack-yi that it's even saved, but only the root node should be
            //	depth 0 and there should only be one (and it's already created)
            if( depth == 0)
                continue

            val node = when( type) {
                SaveLoadUtil.NODE_GROUP -> {
                    workspace.groupTree.addGroupNode(nodeLayer[depth - 1], name)
                            .apply {
                                nodeLayer[depth] = this
                                this.flattened = flatenned
                            }
                }
                else -> {
                    val layer = LayerLoaderFactory.getLayerLoader(context.version, type)
                            .loadLayer(context, name)
                    if( layer != null)
                        workspace.groupTree.importLayer(nodeLayer[depth-1], name, layer, true)
                    else
                        null
                }
            }

            if( node != null) {
                context.nodes.add(node)
                node.expanded = expanded

                if( context.version < 0x0001_0010) {
                    node.alpha = alpha
                    node.visible = visble
                    node.x = x
                    node.y = y
                }
            }
        }


        // Link the reference nodes (needs to be done afterwards because it might groupLink to a node yet
        //	added to the node map since nodeIDs are based on depth-first Group Tree order,
        //	not creation order) (TODO once ReferenceLayers are in)
    }

    private fun parseAnimationData(context: LoadContext, chunkSize: Int)
    {
        val ra = context.ra
        val endPointer = ra.filePointer + chunkSize

        while( ra.filePointer < endPointer) {
            val name = if( context.version == 0x1_0000) "animation" else SaveLoadUtil.readNullTerminatedStringUTF8(ra)
            val speed = if( context.version < 0x1_0005) 8.0f else ra.readFloat()
            val zoom = if( context.version < 0x1_000C) 1 else ra.readShort().i
            val type = ra.readByte().i

            val animation = AnimationLoaderFactory
                    .getAnimationLoader(context.version, type)
                    .loadAnimation(context, name)

            if( animation != null) {
                context.animations.add(animation)
                context.workspace.animationManager.addAnimation(animation)
                val stateBind = context.workspace.animationStateSvc.getState(animation)
                stateBind.speed = speed
                stateBind.zoom = zoom
            }
        }
    }

    private fun parsePaletteData(context: LoadContext, chunkSize: Int)
    {
        val ra = context.ra
        val endPointer = ra.filePointer + chunkSize

        while( ra.filePointer < endPointer) {
            val name = SaveLoadUtil.readNullTerminatedStringUTF8(ra)
            val size = ra.readUnsignedShort()
            val data = ByteArray(size).also { ra.read(it) }
            context.workspace.paletteSet.addPalette(name, false, data)
            context.workspace.paletteSet.removePalette(0)
        }
    }

    private fun parsePaletteMapData(context: LoadContext, chunkSize: Int)
    {
        val ra = context.ra

        val numMappedNodes = ra.readInt()
        val nodeMap = List(numMappedNodes) {
                    val node = context.nodes.getOrNull(ra.readInt())
                    val colorSize = ra.readUnsignedByte()
                    val colors = List<Color>(colorSize) { ColorARGB32Normal(ra.readInt()) }

                    node?.run { Pair(this,colors) }
                }
                .filterNotNull()
                .toMap()

        val numSpriteMaps = ra.readInt()
        val spriteMap = List(numSpriteMaps) {
                    val group = context.nodes.getOrNull(ra.readInt()) as? GroupNode
                    val partName = ra.readUTF8NT()
                    val colorSize = ra.readUnsignedByte()
                    val colors = List<Color>(colorSize) { ColorARGB32Normal(ra.readInt()) }

                    group?.run { Pair(Pair(group, partName), colors) }
                }
                .filterNotNull()
                .toMap()

        context.workspace.paletteMediumMap.import(nodeMap, spriteMap)
    }

    private fun parseAnimationSpaceData(context: LoadContext, chunkSize: Int)
    {
        val ra = context.ra
        val endPointer = ra.filePointer + chunkSize

        while( ra.filePointer < endPointer) {
            val name = ra.readUTF8NT()
            val type = ra.readByte().i

            val space = AnimationSpaceLoaderFactory
                    .getAnimationSpaceLoader(context.version, type)
                    .loadAnimationSpace(context, name)

            if( space != null)
                context.workspace.animationSpaceManager.addAnimationSpace(space, true)
        }
    }
}


class BadSifFileException(message: String) : Exception(message)