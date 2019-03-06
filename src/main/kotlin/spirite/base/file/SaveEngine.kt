package spirite.base.file

import rb.vectrix.linear.Vec2i
import spirite.base.file.SaveLoadUtil.FFALAYER_GROUPLINKED
import spirite.base.file.SaveLoadUtil.FFALAYER_LEXICAL
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FFAFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FFALayerGroupLinked
import spirite.base.imageData.animation.ffa.FFALayerLexical
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.magLev.MaglevFill
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType
import spirite.hybrid.MDebug.WarningType.STRUCTURAL
import spirite.hybrid.MDebug.WarningType.UNSUPPORTED
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.math.min

class SaveContext(
        val workspace: IImageWorkspace,
        val ra: RandomAccessFile)
{
    val nodeMap = mutableMapOf<Node, Int>()
    val animationMap = mutableMapOf<Animation,Int>()

    val root = workspace.groupTree.root
    val floatingData = workspace.mediumRepository.dataList
            .mapNotNull { workspace.mediumRepository.floatData(it) { medium -> MediumPreparer.prepare(medium)} }


    inline fun writeChunk( tag: String, writer : (RandomAccessFile)->Unit) {
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
        if( workspace.animationManager.animations.any())
            saveAnimationData(context)
        if( workspace.animationSpaceManager.animationSpaces.any())
            saveAnimationSpaceChunk(context)
        savePaletteData(context)

        ra.close()
    }

    private fun saveHeader(context: SaveContext) {
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
    private fun saveGroupTree( context: SaveContext)
    {
        context.writeChunk("GRPT") {ra->
            val depth = 0
            var met = 0

            fun buildReferences( node: Node, depth: Int) {
                // Fills out a map from nodes to an int identifier (constructed here) for use in any nodes that
                //  reference other nodes (such as ReferenceLayers)
                context.nodeMap.put( node, met++)
                (node as? GroupNode)?.children?.forEach { buildReferences(it, depth+1) }
            }
            fun writeNode( node: Node, depth: Int) {
                // [1] : Depth of Node in GroupTree
                ra.writeByte(depth)

                // [4] : alpha
                ra.writeFloat( node.alpha)

                // [2] : xi offset, [2] : yi offset
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
                                // [1] : NodeTypeId
                                ra.writeByte(SaveLoadUtil.NODE_SPRITE_LAYER)

                                val parts = layer.parts.toList()

                                // [1] : Number of parts
                                ra.writeByte( parts.size)

                                parts.forEach { part ->
                                    // Per Part:
                                    ra.write(SaveLoadUtil.strToByteArrayUTF8( part.partName)) // n : PartTypeName
                                    ra.writeFloat(part.transX)  // 4 : TranslationX
                                    ra.writeFloat(part.transY)  // 4 : TranslationY
                                    ra.writeFloat(part.scaleX)  // 4 : ScaleX
                                    ra.writeFloat(part.scaleY)  // 4 : ScaleY
                                    ra.writeFloat(part.rot)     // 4 : rotation
                                    ra.writeInt(part.depth)     // 4 : draw depth
                                    ra.writeInt(part.handle.id) // 4 : MediumId
                                    ra.writeFloat(part.alpha)   // 4 : Alpha
                                }
                            }
                            //is ReferenceLayer -> {}
                            //is PuppetLayer -> {}
                        }
                    }
                }

            }

            buildReferences(context.workspace.groupTree.root, depth)
            writeNode(context.root, 0)
        }
    }

    /** PLTT chunk containing the Palettes saved in the workspace */
    private fun savePaletteData( context: SaveContext) {
        context.writeChunk("PLTT") {ra ->
            context.workspace.paletteSet.palettes.forEach {
                // [n], UTF8 : Palette Name
                ra.write( SaveLoadUtil.strToByteArrayUTF8(it.name))

                val raw = it.compress()
                ra.writeShort(raw.size) // [2] Palette Data Size
                ra.write(raw)           // [n] Compressed Palette Data
            }
        }
    }

    /** IMGD chunk containing all Medium Data, with images saved in PNG Format*/
    private fun saveImageData( context: SaveContext) {
        context.writeChunk("IMGD") {ra->
            context.floatingData.forEach { floatingMedium ->
                // [4] : Medium Handle Id
                ra.writeInt( floatingMedium.id)

                val prepared = floatingMedium.condensed
                when( prepared) {
                    is PreparedFlatMedium -> {
                        ra.writeByte(SaveLoadUtil.MEDIUM_PLAIN) // [1] : Medium Type

                        val byteArray = Hybrid.imageIO.writePNG(prepared.image)
                        ra.writeInt( byteArray.size)    // [4] : Size of Image Data
                        ra.write(byteArray)             // [n] : Image Data
                    }
                    is PreparedDynamicMedium -> {
                        ra.writeByte(SaveLoadUtil.MEDIUM_DYNAMIC) // [1] : Medium Type
                        ra.writeShort( prepared.offsetX)        // [2] : Dynamic AnimationCommand Offset
                        ra.writeShort( prepared.offsetY)        // [2] : Dynamic Y Offset

                        val byteArray = prepared.image?.run { Hybrid.imageIO.writePNG(this)}
                        ra.writeInt( byteArray?.size ?: 0)  // [4] : Size of Image Data (note: May be 0)
                        byteArray?.run { ra.write(this)}    // [n] : Image Data
                    }
                    is PreparedMaglevMedium -> {
                        ra.writeByte(SaveLoadUtil.MEDIUM_MAGLEV)    // [1] : Medium Type
                        ra.writeShort(prepared.things.size)     // [2] : number of things
                        prepared.things.forEach {thing ->
                            when(thing) {
                                is MaglevStroke -> {
                                    ra.writeByte(SaveLoadUtil.MAGLEV_THING_STROKE)  // [1] : Thing type
                                    ra.writeInt(thing.params.color.argb32)             // [4] : Color
                                    ra.writeByte(thing.params.method.fileId)            // [1] : Method
                                    ra.writeFloat(thing.params.width)                  // [4] : Stroke Width
                                    ra.writeByte(thing.params.mode.fileId)        // [1] : Mode
                                    ra.writeInt(thing.drawPoints.length)     // [4] : Num Vertices

                                    ra.writeFloatArray(thing.drawPoints.x)
                                    ra.writeFloatArray(thing.drawPoints.y)
                                    ra.writeFloatArray(thing.drawPoints.w)
                                }
                                is MaglevFill -> {
                                    ra.writeByte(SaveLoadUtil.MAGLEV_THING_FILL)    // [1] : ThingType
                                    ra.writeInt(thing.color.argb32) // [4] : Color
                                    ra.writeByte(thing.mode.fileId) // [1] : FillMethod

                                    val numSegs = min(65535, thing.segments.size)
                                    ra.writeShort(numSegs)  // [2] : Num Segments
                                    thing.segments.asSequence().take(numSegs).forEach { seg ->
                                        ra.write(seg.strokeId)   // [4]: StrokeId
                                        ra.write(seg.start)  // [4] : Start
                                        ra.write(seg.end)    // [4]
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveAnimationSpaceChunk(context: SaveContext) {
        val animMap = context.animationMap

        context.writeChunk("ANSP") {ra ->
            context.workspace.animationSpaceManager.animationSpaces.forEach { space ->
                ra.writeUFT8NT(space.name)

                when( space) {
                    is FFAAnimationSpace -> {
                        ra.writeByte(SaveLoadUtil.ANIMSPACE_FFA) // [1] : Type

                        val animations = space.animationStructs.toList()
                        val links = space.links.toList()

                        ra.writeShort(animations.size)  // [2] : Number of Animations
                        animations.forEach { struct ->
                            val anim = struct.animation
                            ra.writeInt(animMap[anim] ?: -1)    // 4: AnimationId
                            val onEnd = struct.onEndLink
                            val onEndLink =  if( onEnd == null) -1 else animMap[onEnd.first] ?: -1

                            ra.writeInt(onEndLink) // 4: AnimationId of on-end groupLink
                            if(onEndLink != -1)
                                ra.writeInt(onEnd!!.second)    // 4: on-end Frame

                            val logSpace = space.stateView.logicalSpace[anim] ?: Vec2i.Zero
                            ra.writeShort(logSpace.xi)    // 2: Logical AnimationCommand
                            ra.writeShort(logSpace.yi)    // 2: Logical Y
                        }

                        ra.writeShort(links.size)   // [2] : Number of Links
                        links.forEach {
                            ra.writeInt(animMap[it.origin] ?: -1)
                            ra.writeInt(it.originFrame)
                            ra.writeInt(animMap[it.destination] ?: -1)
                            ra.writeInt(it.destinationFrame)
                        }
                    }
                    else -> {
                        ra.writeByte(0)
                        MDebug.handleWarning(UNSUPPORTED, "Do not know how to save Animation Space: $space.  Skipping it")
                    }
                }
            }
        }
    }

    private const val MaxFFALayers = Short.MAX_VALUE.toInt()
    private const val MaxFFALayerFrames = Short.MAX_VALUE.toInt()

    /** ANIM chunk containing all Animation Data */
    private fun saveAnimationData( context: SaveContext) {
        context.writeChunk("ANIM") {ra ->
            var met = 0

            context.workspace.animationManager.animations.forEach { anim ->
                context.animationMap[anim] = met++

                when( anim) {
                    is FixedFrameAnimation -> {
                        ra.write(SaveLoadUtil.strToByteArrayUTF8(anim.name))    // [n] Anim name
                        ra.writeFloat(anim.state.speed)                         // [4] : Anim Speed
                        ra.writeByte(SaveLoadUtil.ANIM_FFA)                     // [1] : Anim TypeId

                        if(anim.layers.size > MaxFFALayers) MDebug.handleWarning(UNSUPPORTED, "Too many Animation layers (num: ${anim.layers.size} max: ${MaxFFALayers}), taking only the first N")

                        val writtenExplicits = hashSetOf<Node>()

                        ra.writeShort( min(anim.layers.size, MaxFFALayers))  // [2] : Number of layers
                        for (layer in anim.layers.asSequence().take(MaxFFALayers)){
                            when(layer) {
                                is FFALayerGroupLinked -> {
                                    ra.writeByte(FFALAYER_GROUPLINKED)  // [1] : Layer TypeId

                                    ra.writeInt(context.nodeMap[layer.groupLink] ?: -1)    // [4] : NodeId of GroupNode Bount
                                    ra.writeByte(if(layer.includeSubtrees) 1 else 0)    // [1] : 0 bit : whether or not subgroups are linked

                                    if( layer.frames.size > MaxFFALayerFrames) MDebug.handleWarning(UNSUPPORTED, "Too many Frames in a layer (max: ${Short.MAX_VALUE}, only writing first N)")

                                    ra.writeShort(min(layer.frames.size, MaxFFALayerFrames)) // [2] : Number of Frames
                                    for( frame in layer.frames.asSequence().take(MaxFFALayerFrames)) {
                                        val type = when(frame.marker) {
                                            GAP -> SaveLoadUtil.FFAFRAME_GAP
                                            START_LOCAL_LOOP -> SaveLoadUtil.FFAFRAME_STARTOFLOOP
                                            FRAME -> SaveLoadUtil.FFAFRAME_FRAME
                                            END_LOCAL_LOOP -> null
                                        } ?: continue
                                        ra.writeByte(type)
                                        ra.writeInt(context.nodeMap[frame.structure.node] ?: -1 ) // [4] : NodeId
                                        ra.writeShort(frame.length)    // [2]: Length
                                    }
                                }
                                is FFALayerLexical -> {
                                    ra.writeByte(FFALAYER_LEXICAL)  // [1] : Layer TypeId
                                    ra.writeInt(context.nodeMap[layer.groupLink] ?: -1) // [4] NodeIf of GroupNode
                                    ra.writeUFT8NT(layer.lexicon)   // [n] : Lexicon

                                    if( writtenExplicits.contains(layer.groupLink)) {
                                        ra.writeByte(0) // [1] : No Explicits to write
                                    }
                                    else {
                                        ra.writeByte(min(255, layer.sharedExplicitMap.size))   // [1] : Num Explicits
                                        for( explicit in layer.sharedExplicitMap.asSequence().take(255)) {
                                            ra.writeByte(explicit.key.toByte().toInt()) // [1] : Char mapping
                                            ra.writeInt(context.nodeMap[explicit.value] ?: -1) // [4] : NodeId
                                        }
                                    }

                                }
                            }

                        }
                    }
                    else -> MDebug.handleWarning(UNSUPPORTED, "Do not know how to save Animation: $anim.  Skipping it")
                }
            }
        }

    }
}