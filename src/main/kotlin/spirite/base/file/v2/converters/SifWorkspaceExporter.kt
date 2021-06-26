package spirite.base.file.v2.converters

import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.b
import sgui.core.systems.IImageIO
import spirite.base.file.SaveLoadUtil
import spirite.base.file.save.MediumPreparer
import spirite.base.file.save.PreparedDynamicMedium
import spirite.base.file.save.PreparedFlatMedium
import spirite.base.file.save.PreparedMaglevMedium
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.*
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.magLev.MaglevFill
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.*
import kotlin.Exception

interface ISifWorkspaceExporter {
    fun export( workspace: IImageWorkspace) : SifFile
}

class SifWorkspaceExporter(
    private val _imgIo : IImageIO
) : ISifWorkspaceExporter
{
    class ExportContext(
        val workspace: IImageWorkspace )
    {
        val nodeMap = mutableMapOf<Node,Int>()
        val animMap = mutableMapOf<Animation, Int>()

        val root = workspace.groupTree.root
        val floatingData = workspace.mediumRepository.dataList
            .mapNotNull { workspace.mediumRepository.floatData(it) { medium -> MediumPreparer.prepare(medium) } }

        fun getNodeId(node: Node?) : Int = nodeMap[node] ?: -1

        init {
            buildNodeMap()
            buildAnimMap()
        }

        private fun buildNodeMap() {
            var met = 0
            fun sub(node: Node, depth: Int) {
                nodeMap[node] = met++
                (node as? GroupNode)?.children?.forEach { sub(it, depth+1) }
            }
            sub(workspace.groupTree.root, 0)
        }

        private fun buildAnimMap() {
            var met = 0
            workspace.animationManager.animations.forEach { animMap[it] = met++ }
        }
    }

    override fun export(workspace: IImageWorkspace): SifFile {
        val context = ExportContext(workspace)

        val grpt = exportGrpt(context)
        val imgd = exportImgd(context)
        val anim = exportAnim(context)
        val pltt = exportPltt(context)
        val tplt = exportTpltChunk(context)
        val ansp = exportAnspChunk(context)
        val view = exportViewChunk(context)

        return SifFile(
            workspace.width,
            workspace.height,
            SifConstants.latestVersion,
            grpt, imgd, anim, pltt, tplt, ansp, view)
    }

    fun exportGrpt(context: ExportContext) : SifGrptChunk{
        val nodeList = mutableListOf<SifGrptNode>()

        fun convertNode(node: Node, depth: Int) {
            val data : SifGrptNodeData = when( node) {
                is GroupNode -> SifGrptNode_Group
                is LayerNode -> when( val layer = node.layer){
                    is SimpleLayer -> {
                        SifGrptNode_Simple(layer.medium.id)
                    }
                    is SpriteLayer -> {
                        SifGrptNode_Sprite(
                            layer.type.permanentCode,
                            layer.parts.map { part ->
                                SifGrptNode_Sprite.Part(
                                    part.partName,
                                    part.transX,
                                    part.transY,
                                    part.scaleX,
                                    part.scaleY,
                                    part.rot,
                                    part.depth,
                                    part.handle.id,
                                    part.alpha)
                            }
                        )

                    }
                    else -> throw SifFileException("Unrecognized Layer Type on node ${node.name}")
                }
                else -> throw SifFileException("Unrecognized Node.  ${node.name}")
            }

            val bitFlags = //if( node.visible) SaveLoadUtil.VisibleMask else 0 or
                        if( node.expanded) SaveLoadUtil.ExpandedMask else 0 or
                        if( node is GroupNode && node.flattened) SaveLoadUtil.FlattenedMask else 0
            nodeList.add(SifGrptNode(
                settingsBitFlag = bitFlags.toByte(),
                name = node.name,
                data = data,
                depth = depth))

            if( node is GroupNode){
                if( depth == 0xFF && node.children.any())
                    throw SifFileException("Dug too greedily and too deep")
                node.children.forEach { convertNode(it, depth+1) }
            }
        }

        convertNode(context.root, 0)

        return SifGrptChunk(nodeList)
    }

    fun exportImgd(context: ExportContext) : SifImgdChunk {
        val mediums = context.floatingData.mapNotNull {  floatingMedium ->
            val data : SifImgdMediumData = when( val prepared = floatingMedium.condensed) {
                is PreparedFlatMedium -> {
                    val raw = _imgIo.writePNG(prepared.image)
                    SifImgdMed_Plain(raw)
                }
                is PreparedDynamicMedium -> {
                    val raw = prepared.image?.run { _imgIo.writePNG(this) }
                        ?: ByteArray(0)

                    SifImgdMed_Dynamic(
                        prepared.offsetX.toShort(),
                        prepared.offsetY.toShort(),
                        raw )
                }
                is PreparedMaglevMedium -> {
                    val raw = prepared.image?.run { _imgIo.writePNG(this) }
                        ?: ByteArray(0)

                    val things : List<SifImgdMagThing> = prepared.things.map { thing -> when( thing) {
                        is MaglevStroke -> {
                            SifImgdMagThing_Stroke(
                                thing.params.color.argb32,
                                thing.params.method.fileId.toByte(),
                                thing.params.width,
                                thing.params.mode.fileId.b,
                                thing.drawPoints.x,
                                thing.drawPoints.y,
                                thing.drawPoints.w )
                        }
                        is MaglevFill -> {
                            SifImgdMagThing_Fill(
                                thing.color.argb32,
                                thing.mode.fileId.b,
                                thing.segments.map { seg->
                                    SifImgdMagThing_Fill.RefPoint(
                                        seg.strokeId,
                                        seg.start,
                                        seg.end )
                                } )
                        }
                        else -> throw Exception("Unrecognized maglev thing")
                    } }

                    SifImgdMed_Maglev(
                        prepared.offsetX.toShort(),
                        prepared.offsetY.toShort(),
                        raw,
                        things )
                }

                else -> return@mapNotNull null
            }

            SifImgdMedium(
                floatingMedium.id,
                data)
        }

        return SifImgdChunk(mediums)
    }

    fun exportAnim(context: ExportContext): SifAnimChunk {
        val nodeMap = context.nodeMap
        val animations = context.workspace.animationManager.animations.map {  anim ->
            val state = context.workspace.animationStateSvc.getState(anim)

            val data : SifAnimAnimData = when( anim) {
                is FixedFrameAnimation -> {
                    val layers = anim.layers.map { layer ->
                        val lData : SifAnimFfaLayerData = when( layer) {
                            is FfaLayerGroupLinked -> {
                                val frames = layer.frames.map { frame ->
                                    SifAnimFfaLayer_Grouped.Frames(
                                        (frame as FFALayer.FFAFrame).marker.fileId.toByte(),
                                        nodeMap[frame.structure.node] ?: -1,
                                        frame.length)
                                }

                                SifAnimFfaLayer_Grouped(
                                    nodeMap[layer.groupLink] ?: -1,
                                    layer.includeSubtrees,
                                    frames )
                            }
                            is FfaLayerLexical -> {
                                val explicitMappings = layer.sharedExplicitMap
                                    .map { Pair(it.key, nodeMap[it.value] ?: -1) }

                                SifAnimFfaLayer_Lexical(
                                    nodeMap[layer.groupLink] ?: -1,
                                    layer.lexicon,
                                    explicitMappings )
                            }
                            is FfaLayerCascading ->{
                                val subLayers = layer.sublayerInfo.map {
                                    SifAnimFfaLayer_Cascading.SubLayer(
                                        nodeMap[it.key] ?: -1,
                                        it.value.primaryLen,
                                        it.value.lexicalKey,
                                        it.value.lexicon ?: "" )
                                }
                                SifAnimFfaLayer_Cascading(
                                    nodeMap[layer.groupLink] ?: -1,
                                    layer.lexicon ?: "",
                                    subLayers )
                            }
                            else -> throw SifFileException("Unsupported FFA Layer Type")
                        }

                        SifAnimFfaLayer(
                            layer.name,
                            layer.asynchronous,
                            lData)
                    }

                    SifAnimAnim_FixedFrame(layers)
                }
                else -> throw SifFileException("Unsupported Animation Type")
            }

            SifAnimAnimation(
                anim.name,
                state.speed,
                state.zoom.toShort(),
                data)
        }

        return SifAnimChunk(animations)
    }

    fun exportPltt(context: ExportContext): SifPlttChunk {
        val palettes = context.workspace.paletteSet.palettes
            .map { palette->
                val raw = palette.compress()
                SifPlttPalette(
                    palette.name,
                    raw )
            }
        return SifPlttChunk(palettes)
    }

    fun exportTpltChunk(context: ExportContext) : SifTpltChunk {
        val nodeColors = context.workspace.paletteMediumMap.getNodeMappings().map { entry ->
            val node = entry.key
            val belt = entry.value

            SifTpltNodeMap(
                context.getNodeId(node),
                belt.map { it.argb32 } )
        }

        val spritePartColors = context.workspace.paletteMediumMap.getSpriteMappings().map { entry ->
            val (node, spritePartName) = entry.key
            val belt = entry.value

            SifTpltSpritePartMap(
                context.getNodeId(node),
                spritePartName,
                belt.map { it.argb32 } )
        }

        return SifTpltChunk(nodeColors, spritePartColors)
    }

    fun exportAnspChunk(context: ExportContext) : SifAnspChunk {
        val animMap = context.animMap
        val spaces = context.workspace.animationSpaceManager.animationSpaces.map { space ->
            if( space !is FFAAnimationSpace)
                throw SifFileException("Unsupported Animation Space")

            val anims = space.animationStructs.map { struct ->
                val onEndLink = struct.onEndLink?.run { animMap[first] } ?: -1
                val logSpace = space.stateView.logicalSpace[struct.animation] ?: Vec2i.Zero
                SifAnspAnim(
                    animMap[struct.animation] ?: -1,
                    onEndLink,
                    struct.onEndLink?.second,
                    logSpace.xi,
                    logSpace.yi )
            }

            val links = space.links.map { link ->
                SifAnspLink(
                    animMap[link.origin] ?: -1,
                    link.originFrame,
                    animMap[link.destination] ?: -1,
                    link.destinationFrame )
            }

            SifAnspSpace(
                space.name,
                anims,
                links )
        }

        return SifAnspChunk(spaces)
    }

    fun exportViewChunk(context: ExportContext) : SifViewChunk {
        val reverseNodeMap = context.nodeMap
            .map { Pair(it.value, it.key) }
            .sortedBy { it.first }
            .toList()
        val viewSystem = context.workspace.viewSystem

        val views = (0 until viewSystem.numActiveViews).map { viewNum->
            val selected = context.getNodeId(viewSystem.getCurrentNode(viewNum))

            val nodeProps = reverseNodeMap.map { (_,node)->
                val nodeProps = viewSystem.get(node, viewNum)
                SifViewView.Properties(
                    if(nodeProps.isVisible) 1 else 0,
                    nodeProps.alpha,
                    nodeProps.method.methodType.ordinal.toByte(),
                    nodeProps.method.renderValue,
                    nodeProps.ox,
                    nodeProps.oy)
            }

            SifViewView(
                selected,
                nodeProps)
        }

        return SifViewChunk(views)
    }

}