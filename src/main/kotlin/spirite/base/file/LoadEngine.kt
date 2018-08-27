package spirite.base.file

import spirite.base.brains.IMasterControl
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace.SpacialLink
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpritePartStructure
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.IMedium.MediumType.*
import spirite.base.util.i
import spirite.base.util.linear.Vec2i
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.UNSUPPORTED
import java.io.File
import java.io.IOException
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
            // Animation Space Data (optional)
            context.chunkInfo.singleOrNull { it.header == "ANSP" }?.apply {
                ra.seek(startPointer)
                parseAnimationSpaceData(context,size)
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
            throw BadSifFileException("Error Reading File: " + e.stackTrace)
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

    private fun parseImageDataSection(context: LoadContext, chunkSize: Int) {
        val dataMap = mutableMapOf<Int,IMedium>()
        val ra = context.ra
        val endPointer = ra.filePointer + chunkSize

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

                    val img = Hybrid.imageIO.loadImage(imgData)
                    dataMap[id] = FlatMedium(img, context.workspace.mediumRepository)
                }
                DYNAMIC -> {
                    val ox = ra.readShort().i
                    val oy = ra.readShort().i
                    val imgSize = ra.readInt()
                    val img = when( imgSize) {
                        0 -> null
                        else -> {
                            val imgData = ByteArray(imgSize).apply { ra.read( this) }
                            Hybrid.imageIO.loadImage(imgData)
                        }
                    }
                    dataMap[id] = DynamicMedium(context.workspace, DynamicImage(img, ox, oy), context.workspace.mediumRepository)
                }
                PRISMATIC -> {
                    MDebug.handleWarning(UNSUPPORTED, "Prismatic Mediums are currently not supported by Spirite v2, ignoring.")
                    val numlayers = ra.readUnsignedShort()
                    repeat(numlayers) {
                        ra.readInt()
                        ra.readShort()
                        ra.readShort()
                        val imgSize = ra.readInt()
                        ra.skipBytes(imgSize)
                    }
                }
                MAGLEV -> {
                    MDebug.handleWarning(UNSUPPORTED, "Maglev Mediums are currently not supported by Spirite v2, ignoring.")
                    val numThings = ra.readUnsignedShort()
                    repeat(numThings) {
                        val thingType = ra.readByte()
                        when( thingType.i) {
                            0 -> { // stroke
                                ra.readInt()
                                ra.readByte()
                                ra.readFloat()
                                val numVertices = ra.readUnsignedShort()
                                repeat(numVertices) {
                                    ra.readFloat()
                                    ra.readFloat()
                                    ra.readFloat()
                                }
                            }
                            1 -> { // fill
                                ra.readInt()
                                ra.readByte()
                                val numReferences = ra.readUnsignedShort()
                                repeat(numReferences) {
                                    ra.readUnsignedShort()
                                    ra.readFloat()
                                    ra.readFloat()
                                }
                            }
                        }
                    }
                }
                DERIVED_MAGLEV -> {}
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

            val name = SaveLoadUtil.readNullTerminatedStringUTF8(ra)
            val type =  ra.readUnsignedByte()

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
                SaveLoadUtil.NODE_SPRITE_LAYER -> {
                    val partSize = ra.readUnsignedByte()
                    val parts = List(partSize) {
                        val partName = SaveLoadUtil.readNullTerminatedStringUTF8(ra)

                        val spritePartStructure = when {
                            context.version <= 4 -> {
                                val transX = ra.readShort().toFloat()
                                val transY = ra.readShort().toFloat()
                                val drawDepth = ra.readInt()
                                SpritePartStructure(drawDepth, partName, true, 1f, transX, transY, 1f, 1f, 0f)
                            }
                            else -> {
                                val transX = ra.readFloat()
                                val transY = ra.readFloat()
                                val scaleX = ra.readFloat()
                                val scaleY = ra.readFloat()
                                val rot = ra.readFloat()
                                val drawDepth = ra.readInt()
                                SpritePartStructure(drawDepth, partName, true, 1f, transX, transY, scaleX, scaleY, rot)

                            }
                        }
                        val medium = MediumHandle(workspace,context.reindex(ra.readInt()))
                        Pair(medium, spritePartStructure)
                    }

                    val sprite = SpriteLayer(workspace, workspace.mediumRepository, parts)

                    workspace.groupTree.importLayer(nodeLayer[depth-1], name, sprite, true)
                }
                SaveLoadUtil.NODE_REFERENCE_LAYER -> {
                    MDebug.handleWarning(UNSUPPORTED, "Reference Layers are currently not supported by Spirite v2, ignoring Refernce Layer")
                    ra.readInt()    // [4] : NodeID
                    null
                }
                SaveLoadUtil.NODE_PUPPET_LAYER -> {
                    MDebug.handleWarning(UNSUPPORTED, "Puppet Layers are currently not supported by Spirite v2, ignoring Puppet Layer")
                    val byte = ra.readByte()   // [1] : Whether or not is derived
                    if( byte.i == 0) {
                        val numParts = ra.readUnsignedShort() // [2] : Number of parts
                        for( i in 0 until numParts) {
                            ra.readShort()  // [2] : Parent
                            ra.readInt()    // [4] : MediumId
                            ra.readFloat()  // [16] : Bone x1, y1, x2, y2
                            ra.readFloat()
                            ra.readFloat()
                            ra.readFloat()
                            ra.readInt()    // [4]: DrawDepth
                        }

                    }
                    else throw BadSifFileException("Do not know how to handle derived puppet types")
                    null
                }
                else -> throw BadSifFileException("Unrecognized Node Type ID: $type (version mismatch or corrupt file?)")
            }

            if( node != null) {
                context.nodes.add(node)
                node.alpha = alpha
                node.expanded = bitmask and SaveLoadUtil.EXPANDED_MASK != 0
                node.visible = bitmask and SaveLoadUtil.VISIBLE_MASK != 0
                node.x = x
                node.y = y
            }
        }


        // Link the reference nodes (needs to be done afterwards because it might link to a node yet
        //	added to the node map since nodeIDs are based on depth-first Group Tree order,
        //	not creation order) (TODO once ReferenceLayers are in)
    }

    private fun parseAnimationData( context: LoadContext, chunkSize: Int)
    {
        val nodes = context.nodes
        val ra = context.ra
        val endPointer = ra.filePointer + chunkSize

        while( ra.filePointer < endPointer) {
            val name = if( context.version == 0x1_0000) "animation" else SaveLoadUtil.readNullTerminatedStringUTF8(ra)
            val type = ra.readByte().i
            val animation = when( type) {
                SaveLoadUtil.ANIM_FFA -> FFALoaderFactory.getLoaderFromVersion(context.version).loadAnimation(context, name)
                SaveLoadUtil.ANIM_RIG -> {
                    MDebug.handleWarning(UNSUPPORTED, "Rig Animations are currently not supported by Spirite v2, ignoring.")
                    val numSprites = ra.readUnsignedShort()
                    repeat(numSprites) {
                        val nodeId = ra.readInt()
                        val numParts = ra.readUnsignedShort()
                        repeat(numParts) {
                            val name = SaveLoadUtil.readNullTerminatedStringUTF8(ra)
                            val numKeyframes = ra.readUnsignedShort()
                            repeat(numKeyframes) {
                                val t = ra.readFloat()
                                val tx = ra.readFloat()
                                val ty = ra.readFloat()
                                val sx = ra.readFloat()
                                val sy = ra.readFloat()
                                val rot = ra.readFloat()
                            }
                        }
                    }

                    null
                }
                else -> throw BadSifFileException("Unrecognized Animation Type ID: $type (version mismatch or corrupt file?)")
            }

            if( animation != null) {
                context.animations.add(animation)
                context.workspace.animationManager.addAnimation(animation)
            }
        }
    }

    private fun parsePaletteData( context: LoadContext, chunkSize: Int)
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

    private fun parseAnimationSpaceData( context: LoadContext, chunkSize: Int)
    {
        val ra = context.ra
        val endPointer = ra.filePointer + chunkSize
        val anims = context.animations

        while( ra.filePointer < endPointer) {
            val name = ra.readNullTerminatedStringUTF8()
            val type = ra.readByte().i
            val space = when(type) {
                0 -> null
                SaveLoadUtil.ANIMSPACE_FFA -> {
                    val space = FFAAnimationSpace(name, context.workspace)

                    val numAnimations = ra.readUnsignedShort()
                    repeat(numAnimations) {
                        val animation = anims.getOrNull(ra.readInt()) as? FixedFrameAnimation

                        val endLinkAnimId = ra.readInt()
                        val endLinkFrame = if( endLinkAnimId != -1) ra.readInt() else 0

                        val logX = ra.readUnsignedShort()
                        val logY = ra.readUnsignedShort()

                        if( animation != null) {
                            space.addAnimation(animation)

                            val endLinkAnim = anims.getOrNull(endLinkAnimId) as? FixedFrameAnimation
                            if( endLinkAnim != null)
                                space.setOnEndBehavior(animation, Pair(endLinkAnim, endLinkFrame))

                            space.stateView.setLogicalSpace(animation, Vec2i(logX,logY))
                        }
                    }

                    val numLinks = ra.readUnsignedShort()
                    repeat(numLinks) {
                        val origin = anims.getOrNull(ra.readInt()) as? FixedFrameAnimation
                        val originFrame = ra.readInt()
                        val destination = anims.getOrNull(ra.readInt()) as? FixedFrameAnimation
                        val destinationFrame = ra.readInt()
                        if( origin != null && destination != null)
                            space.addLink(SpacialLink(origin, originFrame, destination, destinationFrame))
                    }

                    space
                }
                else -> throw BadSifFileException("Unrecognized AnimationSpace Type ID: $type (version mismatch or corrupt file?)")
            }

            if( space != null)
                context.workspace.animationSpaceManager.addAnimationSpace(space, true)
        }
    }
}


class BadSifFileException(message: String) : Exception(message)