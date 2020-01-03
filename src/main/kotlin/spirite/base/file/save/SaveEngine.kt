package spirite.base.file.save

import rb.alexandria.io.IWriteStream
import rb.clicker.telemetry.TelemetryEvent
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.d
import spirite.base.file.SaveLoadUtil
import spirite.base.file.SaveLoadUtil.FFALAYER_CASCADING
import spirite.base.file.SaveLoadUtil.FFALAYER_GROUPLINKED
import spirite.base.file.SaveLoadUtil.FFALAYER_LEXICAL
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.base.imageData.animation.ffa.FfaFrameStructure.Marker.*
import spirite.base.imageData.animation.ffa.FfaLayerCascading
import spirite.base.imageData.animation.ffa.FfaLayerGroupLinked
import spirite.base.imageData.animation.ffa.FfaLayerLexical
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
import kotlin.math.min

class SaveContext(
        val workspace: IImageWorkspace,
        val ws: IWriteStream)
{
    val nodeMap = mutableMapOf<Node, Int>()
    val animationMap = mutableMapOf<Animation,Int>()
    val telemetry = TelemetryEvent()

    val root = workspace.groupTree.root
    val floatingData = workspace.mediumRepository.dataList
            .mapNotNull { workspace.mediumRepository.floatData(it) { medium -> MediumPreparer.prepare(medium) } }


    inline fun writeChunk(tag: String, crossinline writer : (IWriteStream)->Unit) {
        telemetry.runAction("Write Chunk: $tag") {
            if (tag.length != 4) {
                // Perhaps overkill, but this really should be a hard truth
                MDebug.handleError(ErrorType.FATAL, "Chunk types must be 4-length")
            }

            // [4] : Chunk Tag
            ws.write(tag.toByteArray(charset("UTF-8")))

            val start = ws.pointer
            // [4] : ChunkLength (placeholder for now
            ws.writeInt(0)

            writer.invoke(ws)

            val end = ws.pointer
            ws.goto(start)
            if (end - start > Integer.MAX_VALUE)
                MDebug.handleError(ErrorType.OUT_OF_BOUNDS, "Image Data Too Big (>2GB).")
            ws.writeInt((end - start - 4).toInt())
            ws.goto(end)

            telemetry.mark("size", (end - start).d)
        }
    }
}

object SaveEngine {

    fun saveWorkspace( file : File, workspace: IImageWorkspace) {
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

        // TODO: Fix this
/*
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
        PaletteMapSaver.savePaletteData(context)

        ra.close()
*/
    }

    private fun saveHeader(context: SaveContext) {
        val ws = context.ws
        val workspace = context.workspace

        // [4] Header
        ws.write(SaveLoadUtil.header)
        // [4] Version
        ws.writeInt(SaveLoadUtil.version)

        // [2] Width, [2] Height
        ws.writeShort(workspace.width)
        ws.writeShort(workspace.height)
    }

    /** GRPT chunk saving the structure of the PrimaryGroupTree and all Nodes/Layers within it */
    private fun saveGroupTree( context: SaveContext)
    {
        context.writeChunk("GRPT") {ws->
            val depth = 0
            var met = 0

            fun buildReferences( node: Node, depth: Int) {
                // Fills out a map from nodes to an int identifier (constructed here) for use in any nodes that
                //  reference other nodes (such as ReferenceLayers)
                context.nodeMap.put( node, met++)
                (node as? GroupNode)?.children?.forEach { buildReferences(it, depth+1) }
            }
            fun writeNode( node: Node, depth: Int) {
                // [1] : Depth of GroupNode in GroupTree
                ws.writeByte(depth)

                // [4] : alpha
                ws.writeFloat( node.alpha)

                // [2] : xi offset, [2] : yi offset
                ws.writeShort(node.x)
                ws.writeShort(node.y)

                // [1] : bitmask
                ws.writeByte(
                        if( node.visible) SaveLoadUtil.VisibleMask else 0 or
                        if( node.expanded) SaveLoadUtil.ExpandedMask else 0 or
                        if( node is GroupNode && node.flatenned) SaveLoadUtil.FlattenedMask else 0)

                // [n], UTF8 : Layer Name
                ws.write(SaveLoadUtil.strToByteArrayUTF8(node.name))

                when( node) {
                    is GroupNode -> {
                        // [1] : NodeTypeId
                        ws.writeByte(SaveLoadUtil.NODE_GROUP)

                        // Go through each of the Group GroupNode's children recursively and save them
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
                                ws.writeByte(SaveLoadUtil.NODE_SIMPLE_LAYER)

                                // [4] : MediumId of Medium attatched to the SimpleLayer
                                ws.writeInt( layer.medium.id)
                            }
                            is SpriteLayer -> {
                                ws.writeByte(SaveLoadUtil.NODE_SPRITE_LAYER) // [1] : NodeTypeId
                                ws.writeByte(layer.type.permanentCode)  // [1] : Sprite LAyer Medium Type

                                val parts = layer.parts.toList()

                                // [1] : Number of parts
                                ws.writeByte( parts.size)

                                parts.forEach { part ->
                                    // Per Part:
                                    ws.write(SaveLoadUtil.strToByteArrayUTF8(part.partName)) // n : PartTypeName
                                    ws.writeFloat(part.transX)  // 4 : TranslationX
                                    ws.writeFloat(part.transY)  // 4 : TranslationY
                                    ws.writeFloat(part.scaleX)  // 4 : ScaleX
                                    ws.writeFloat(part.scaleY)  // 4 : ScaleY
                                    ws.writeFloat(part.rot)     // 4 : rotation
                                    ws.writeInt(part.depth)     // 4 : draw depth
                                    ws.writeInt(part.handle.id) // 4 : MediumId
                                    ws.writeFloat(part.alpha)   // 4 : Alpha
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
        context.writeChunk("PLTT") {ws ->
            context.workspace.paletteSet.palettes.forEach {
                // [n], UTF8 : Palette Name
                ws.write(SaveLoadUtil.strToByteArrayUTF8(it.name))

                val raw = it.compress()
                ws.writeShort(raw.size) // [2] Palette Data Size
                ws.write(raw)           // [n] Compressed Palette Data
            }
        }
    }

    /** IMGD chunk containing all Medium Data, with images saved in PNG Format*/
    private fun saveImageData( context: SaveContext) {
        context.writeChunk("IMGD") {ws->
            context.floatingData.forEach { floatingMedium ->
                // [4] : Medium Handle Id
                ws.writeInt( floatingMedium.id)

                val prepared = floatingMedium.condensed
                when( prepared) {
                    is PreparedFlatMedium -> {
                        ws.writeByte(SaveLoadUtil.MEDIUM_PLAIN) // [1] : Medium Type

                        val byteArray = Hybrid.imageIO.writePNG(prepared.image)
                        ws.writeInt( byteArray.size)    // [4] : Size of Image Data
                        ws.write(byteArray)             // [n] : Image Data
                    }
                    is PreparedDynamicMedium -> {
                        ws.writeByte(SaveLoadUtil.MEDIUM_DYNAMIC) // [1] : Medium Type
                        ws.writeShort( prepared.offsetX)        // [2] : Dynamic AnimationCommand Offset
                        ws.writeShort( prepared.offsetY)        // [2] : Dynamic Y Offset

                        val byteArray = prepared.image?.run { Hybrid.imageIO.writePNG(this)}
                        ws.writeInt( byteArray?.size ?: 0)  // [4] : Size of Image Data (note: May be 0)
                        byteArray?.run { ws.write(this)}    // [n] : Image Data
                    }
                    is PreparedMaglevMedium -> {
                        ws.writeByte(SaveLoadUtil.MEDIUM_MAGLEV)    // [1] : Medium Type
                        ws.writeShort(prepared.things.size)     // [2] : number of things
                        prepared.things.forEach {thing ->
                            when(thing) {
                                is MaglevStroke -> {
                                    ws.writeByte(SaveLoadUtil.MAGLEV_THING_STROKE)  // [1] : Thing type
                                    ws.writeInt(thing.params.color.argb32)             // [4] : Color
                                    ws.writeByte(thing.params.method.fileId)            // [1] : Method
                                    ws.writeFloat(thing.params.width)                  // [4] : Stroke Width
                                    ws.writeByte(thing.params.mode.fileId)        // [1] : Mode
                                    ws.writeInt(thing.drawPoints.length)     // [4] : Num Vertices

                                    ws.writeFloatArray(thing.drawPoints.x)
                                    ws.writeFloatArray(thing.drawPoints.y)
                                    ws.writeFloatArray(thing.drawPoints.w)
                                }
                                is MaglevFill -> {
                                    ws.writeByte(SaveLoadUtil.MAGLEV_THING_FILL)    // [1] : ThingType
                                    ws.writeInt(thing.color.argb32) // [4] : Color
                                    ws.writeByte(thing.mode.fileId) // [1] : FillMode

                                    val numSegs = min(65535, thing.segments.size)
                                    ws.writeShort(numSegs)  // [2] : Num Segments
                                    thing.segments.forEach { seg ->
                                        ws.writeInt(seg.strokeId)   // [4]: StrokeId
                                        ws.writeInt(seg.start)  // [4] : Start
                                        ws.writeInt(seg.end)    // [4]
                                    }
                                }
                            }
                        }
                        val byteArray = prepared.image?.run { Hybrid.imageIO.writePNG(this)}
                        ws.writeInt( byteArray?.size ?: 0)  // [4] : Size of Image Data
                        ws.writeShort( prepared.offsetX)        // [2] : Dynamic AnimationCommand Offset
                        ws.writeShort( prepared.offsetY)        // [2] : Dynamic Y Offset
                        byteArray?.run { ws.write(this)}    // [n] : Image Data
                    }
                }
            }
        }
    }

    private fun saveAnimationSpaceChunk(context: SaveContext) {
        val animMap = context.animationMap

        context.writeChunk("ANSP") {ws ->
            context.workspace.animationSpaceManager.animationSpaces.forEach { space ->
                ws.writeStringUft8Nt(space.name)

                when( space) {
                    is FFAAnimationSpace -> {
                        ws.writeByte(SaveLoadUtil.ANIMSPACE_FFA) // [1] : Type

                        val animations = space.animationStructs.toList()
                        val links = space.links.toList()

                        ws.writeShort(animations.size)  // [2] : Number of Animations
                        animations.forEach { struct ->
                            val anim = struct.animation
                            ws.writeInt(animMap[anim] ?: -1)    // 4: AnimationId
                            val onEnd = struct.onEndLink
                            val onEndLink =  if( onEnd == null) -1 else animMap[onEnd.first] ?: -1

                            ws.writeInt(onEndLink) // 4: AnimationId of on-end groupLink
                            if(onEndLink != -1)
                                ws.writeInt(onEnd!!.second)    // 4: on-end Frame

                            val logSpace = space.stateView.logicalSpace[anim] ?: Vec2i.Zero
                            ws.writeShort(logSpace.xi)    // 2: Logical AnimationCommand
                            ws.writeShort(logSpace.yi)    // 2: Logical Y
                        }

                        ws.writeShort(links.size)   // [2] : Number of Links
                        links.forEach {
                            ws.writeInt(animMap[it.origin] ?: -1)
                            ws.writeInt(it.originFrame)
                            ws.writeInt(animMap[it.destination] ?: -1)
                            ws.writeInt(it.destinationFrame)
                        }
                    }
                    else -> {
                        ws.writeByte(0)
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
        context.writeChunk("ANIM") {ws ->
            var met = 0

            context.workspace.animationManager.animations.forEach { anim ->
                context.animationMap[anim] = met++

                when( anim) {
                    is FixedFrameAnimation -> {
                        ws.write(SaveLoadUtil.strToByteArrayUTF8(anim.name))    // [n] Anim name
                        ws.writeFloat(anim.state.speed)                         // [4] : Anim Speed
                        ws.writeShort(anim.state.zoom)                          // [2] : Anim Zoom
                        ws.writeByte(SaveLoadUtil.ANIM_FFA)                     // [1] : Anim TypeId

                        if(anim.layers.size > MaxFFALayers) MDebug.handleWarning(UNSUPPORTED, "Too many Animation layers (num: ${anim.layers.size} max: $MaxFFALayers), taking only the first N")

                        val writtenExplicits = hashSetOf<Node>()

                        ws.writeShort( min(anim.layers.size, MaxFFALayers))  // [2] : Number of layers
                        for (layer in anim.layers.asSequence().take(MaxFFALayers)){
                            ws.writeStringUft8Nt(layer.name)  // [n] : Layer Name
                            ws.writeByte( if(layer.asynchronous) 1 else 0)  // [1] : IsAsynchronous
                            when(layer) {
                                is FfaLayerGroupLinked -> {
                                    ws.writeByte(FFALAYER_GROUPLINKED)  // [1] : Layer TypeId

                                    ws.writeInt(context.nodeMap[layer.groupLink] ?: -1)    // [4] : NodeId of GroupNode Bount
                                    ws.writeByte(if(layer.includeSubtrees) 1 else 0)    // [1] : 0 bit : whether or not subgroups are linked

                                    if( layer.frames.size > MaxFFALayerFrames) MDebug.handleWarning(UNSUPPORTED, "Too many Frames in a layer (max: ${Short.MAX_VALUE}, only writing first N)")

                                    ws.writeShort(min(layer.frames.size, MaxFFALayerFrames)) // [2] : Number of Frames
                                    for( frame in layer.frames.asSequence().take(MaxFFALayerFrames)) {
                                        val type = when((frame as FFAFrame).marker) {
                                            GAP -> SaveLoadUtil.FFAFRAME_GAP
                                            START_LOCAL_LOOP -> SaveLoadUtil.FFAFRAME_STARTOFLOOP
                                            FRAME -> SaveLoadUtil.FFAFRAME_FRAME
                                            END_LOCAL_LOOP -> null
                                        } ?: continue
                                        ws.writeByte(type)
                                        ws.writeInt(context.nodeMap[frame.structure.node] ?: -1 ) // [4] : NodeId
                                        ws.writeShort(frame.length)    // [2]: Length
                                    }
                                }
                                is FfaLayerLexical -> {
                                    ws.writeByte(FFALAYER_LEXICAL)  // [1] : Layer TypeId
                                    ws.writeInt(context.nodeMap[layer.groupLink] ?: -1) // [4] NodeIf of GroupNode
                                    ws.writeStringUft8Nt(layer.lexicon)   // [n] : Lexicon

                                    if( writtenExplicits.contains(layer.groupLink)) {
                                        ws.writeByte(0) // [1] : No Explicits to write
                                    }
                                    else {
                                        ws.writeByte(min(255, layer.sharedExplicitMap.size))   // [1] : Num Explicits
                                        for( explicit in layer.sharedExplicitMap.asSequence().take(255)) {
                                            ws.writeByte(explicit.key.toByte().toInt()) // [1] : Char mapping
                                            ws.writeInt(context.nodeMap[explicit.value] ?: -1) // [4] : NodeId
                                        }
                                    }
                                }
                                is FfaLayerCascading -> {
                                    ws.writeByte(FFALAYER_CASCADING)    // [1] : Layer TypeId
                                    ws.writeInt(context.nodeMap[layer.groupLink] ?: -1) // [4] NodeId of GroupNode
                                    ws.writeStringUft8Nt(layer.lexicon ?: "") // [n] : Lexicon

                                    val subinfos = layer.sublayerInfo.values.toList()
                                    ws.writeByte(min(255, subinfos.size)) // [1] : Group Subinfos
                                    subinfos.forEach {
                                        ws.writeInt(context.nodeMap[it.group] ?: -1)    // [4] NodeId
                                        ws.writeShort(it.primaryLen)                        // [2] PrimaryLen
                                        ws.writeByte(it.lexicalKey.toByte().toInt())        // [1] Lexical Key
                                        ws.writeStringUft8Nt(it.lexicon ?: "")
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