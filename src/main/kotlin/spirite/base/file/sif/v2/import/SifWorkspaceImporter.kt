package spirite.base.file.sif.v2.import

import rb.global.ILogger
import rb.glow.ColorARGB32Normal
import rb.glow.gle.RenderMethod
import rb.glow.gle.RenderMethodType
import rb.vectrix.interpolation.CubicSplineInterpolator2D
import rb.vectrix.linear.Vec2i
import rb.vectrix.mathUtil.i
import sgui.core.systems.IImageIO
import spirite.base.brains.IMasterControl
import spirite.base.brains.toolset.MagneticFillMode
import spirite.base.brains.toolset.PenDrawMode
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.*
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpritePartStructure
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium
import spirite.base.imageData.mediums.MediumType
import spirite.base.imageData.mediums.magLev.IMaglevThing
import spirite.base.imageData.mediums.magLev.MaglevFill
import spirite.base.imageData.mediums.magLev.MaglevMedium
import spirite.base.imageData.mediums.magLev.MaglevStroke
import spirite.base.imageData.view.NodeViewProperties
import spirite.base.pen.PenState
import spirite.base.pen.stroke.BasicDynamics
import spirite.base.pen.stroke.DrawPoints
import spirite.base.pen.stroke.DrawPointsBuilder
import spirite.base.pen.stroke.StrokeParams
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.*

class SifWorkspaceImporter(
    private val _imageIo : IImageIO,
    private val _logger: ILogger )
{
    class ImportContext(val sif:SifFile){
        lateinit var mediumReindexingMap : Map<Int,Int>
        val nodes: MutableList<Node> = mutableListOf()
        val animations: MutableList<Animation> = mutableListOf()

        fun reindex( index : Int) = mediumReindexingMap[index] ?: throw SifFileException("Medium Id $index does not correspond to any Medium Data")
    }

    fun import(file: SifFile, master: IMasterControl) : MImageWorkspace{
        val workspace = master.createWorkspace(file.width, file.height)
        val paletteDriving = workspace.paletteManager.drivePalette
        workspace.paletteManager.drivePalette = false

        val context = ImportContext(file)

        importImgd(workspace, file.imgdChunk, context)
        importGrpt(workspace, file.grptChunk, context)
        importAnim(workspace, file.animChunk, context)
        importAnsp(workspace, file.anspChunk, context)
        importPltt(workspace, file.plttChunk, context)
        importTplt(workspace, file.tpltChunk, context)
        importView(workspace, file.viewChunk, context)

        return workspace
    }

    fun importImgd(workspace: MImageWorkspace, imgd: SifImgdChunk, context: ImportContext)  {
        val dataMap = mutableMapOf<Int, IMedium>()

        for (medium in imgd.mediums) {
            val med : IMedium = when( val data = medium.data) {
                is SifImgdMed_Plain -> {
                    val img = _imageIo.loadImage(data.rawImgData)
                    FlatMedium(img, workspace.mediumRepository)
                }
                is SifImgdMed_Dynamic -> {
                    val img = data.rawImgData?.run { _imageIo.loadImage(this)}
                    DynamicMedium(workspace, DynamicImage(img, data.offsetX.i, data.offsetY.i))
                }
                is SifImgdMed_Maglev -> {
                    val img = if(data.rawImgData.isEmpty()) null
                        else _imageIo.loadImage(data.rawImgData)
                    val dynamicImage = DynamicImage(img, data.offsetX.i, data.offsetY.i)

                    val things = data.things.map<SifImgdMagThing,IMaglevThing> { thing -> when( thing )  {
                        is SifImgdMagThing_Stroke -> {
                            val params = StrokeParams(
                                color = ColorARGB32Normal(thing.color),
                                method = StrokeParams.Method.fromFileId(thing.method.i) ?: StrokeParams.Method.BASIC.also { _logger.logWarning("Mis-Mapped Method, defaulting to BASIC.  ${thing.method}") },
                                mode = PenDrawMode.fromFileId(thing.drawMode.i) ?: PenDrawMode.NORMAL.also { _logger.logWarning("Mis-Mapped Draw Mode, defaulting to NORMAL.  ${thing.drawMode}") },
                                width = thing.width )

                            val drawPoints =
                                if( thing.preInterpolated) DrawPoints(thing.xs, thing.ys, thing.ws)
                                else {
                                    val penStates = List(thing.xs.size) {
                                        PenState(thing.xs[it], thing.ys[it], thing.ws[it])
                                    }
                                    DrawPointsBuilder.buildPoints(CubicSplineInterpolator2D(), penStates , BasicDynamics )
                                }

                            MaglevStroke(params, drawPoints)
                        }
                        is SifImgdMagThing_Fill -> {
                            val segments = thing.refPoints.map {
                                MaglevFill.StrokeSegment(it.strokeRef, it.startPoint, it.endPoint)
                            }
                            val method = MagneticFillMode.fromFileId(thing.medhod.i) ?: MagneticFillMode.BEHIND.also { _logger.logWarning("Mis-Mapped Magnetic Fill Mode, defaulting to BEHIND.  ${thing.medhod}") }

                            MaglevFill(segments, method, ColorARGB32Normal(thing.color))
                        }
                    } }

                    val thingMap = things.mapIndexed { i, thing -> Pair(i,thing) }.toMap()
                    MaglevMedium(workspace, thingMap, dynamicImage, medium.id)

                }
                is SifImgdMed_Prismatic -> {
                    _logger.logWarning("Encountered Prismatic Medium; Ignoring")
                    null
                }
            } ?: continue

            dataMap[medium.id] = med
        }

        context.mediumReindexingMap = workspace.mediumRepository.importMap(dataMap)
    }

    fun importGrpt(workspace: MImageWorkspace, grpt: SifGrptChunk, context: ImportContext) {
        // Create an array that keeps track of the active layers of group nodes
        //  (all the nested nodes leading up to the current node)
        val nodeLayer = Array<GroupNode?>(256) {null}
        context.nodes.add( workspace.groupTree.root)
        nodeLayer[0] = workspace.groupTree.root

        for (fNode in grpt.nodes) {
            // !!!! Kind of hack-yi that it's even saved, but only the root node should be
            //	depth 0 and there should only be one (and it's already created)
            if( fNode.depth == 0)
                continue
            val bitFlag = fNode.settingsBitFlag.i

            val node = when( val data = fNode.data) {
                is SifGrptNode_Group -> {
                    workspace.groupTree.addGroupNode(nodeLayer[fNode.depth -1], fNode.name). apply {
                        nodeLayer[fNode.depth] = this
                        flattened = bitFlag and SifConstants.FlattenedMask != 0
                    }
                }
                is SifGrptNode_Simple -> {
                    val index = context.reindex(data.mediumId)
                    val layer = SimpleLayer(MediumHandle(workspace, index))
                    workspace.groupTree.importLayer(nodeLayer[fNode.depth - 1], fNode.name, layer, true)
                }
                is SifGrptNode_Sprite -> {
                    val type = MediumType.fromCode(data.layerType) ?: MediumType.DYNAMIC.also { _logger.logWarning("Layer Type mis-mapping.  Defaulting to DYNAMIC. ${data.layerType}") }
                    val parts = data.parts.map {  fPart ->
                        val partStruct = fPart.run {
                            SpritePartStructure(drawDepth, partTypeName, true, alpha, transX, transY, scaleX, scaleY, rotation)
                        }
                        val medium = MediumHandle(workspace, context.reindex( fPart.mediumId))
                        Pair(medium, partStruct)
                    }

                    val layer = SpriteLayer(workspace, parts, type)
                    workspace.groupTree.importLayer(nodeLayer[fNode.depth - 1], fNode.name, layer, true)
                }
                is SifGrptNode_Reference -> {
                    _logger.logWarning("Ignoring Reference-type Node")
                    null
                }
            } ?: continue

            context.nodes.add(node)
            node.expanded = (bitFlag and SifConstants.ExpandedMask) != 0

            if( context.sif.version < 0x1_0010){
                // In newer versions, this is part of the view system
                node.alpha = fNode.obs_alpha ?: 1f
                node.visible = (bitFlag and SifConstants.VisibleMask != 0)
                node.x = fNode.obs_xOffset ?: 0
                node.y = fNode.obs_yOffset ?: 0
            }
        }

    }

    fun importAnim(workspace: MImageWorkspace, anim: SifAnimChunk, context: ImportContext) {
        for (animation in anim.animations) {
            val data = animation.data
            if(data !is SifAnimAnim_FixedFrame) {
                _logger.logWarning("Only Fixed Frame Animations supported; skipping animation ${animation.name}")
                continue
            }

            // FFA Specific Stuff
            val ffa = FixedFrameAnimation(animation.name, workspace)

            fun loadGroupedLayer( lData: SifAnimFfaLayer_Grouped, layer: SifAnimFfaLayer) : IFfaLayer?{
                val groupNode = context.nodes.getOrNull(lData.groupNodeId) as? GroupNode ?: return null
                val frameMap = mutableMapOf<Node, FfaFrameStructure>()
                val unlinkedFrameClusters = mutableListOf<FfaLayerGroupLinked.UnlinkedFrameCluster>()

                var workingNode : Node? = null
                var workingUnlinkedFrames = mutableListOf<FfaFrameStructure>()

                for (frame in lData.frames) {
                    val frameNode = context.nodes.getOrNull(frame.nodeId)

                    val marker = FfaFrameStructure.Marker.values()
                        .firstOrNull { it.fileId == frame.type.i }
                        ?: FfaFrameStructure.Marker.GAP

                    if( frameNode == null)
                        workingUnlinkedFrames.add(FfaFrameStructure(null, marker, frame.len))
                    else {
                        frameMap[frameNode] = FfaFrameStructure(frameNode, marker, frame.len)
                        if( workingUnlinkedFrames.any())
                            unlinkedFrameClusters.add(FfaLayerGroupLinked.UnlinkedFrameCluster(workingNode, workingUnlinkedFrames))
                        workingUnlinkedFrames = mutableListOf()
                        workingNode = frameNode
                    }
                }
                return ffa.addLinkedLayer(groupNode, lData.subgroupsLinked, layer.partTypeName, frameMap, unlinkedFrameClusters)
            }

            fun loadLexicalLayer( lData: SifAnimFfaLayer_Lexical, layer: SifAnimFfaLayer) : IFfaLayer? {
                val groupNode = context.nodes.getOrNull(lData.groupedNodeId) as? GroupNode ?: return null
                val converted = lData.explicitMapping
                    .mapNotNull {
                        val node = context.nodes.getOrNull(it.second) ?: return@mapNotNull null
                        Pair( it.first, node)
                    }
                val map = if( converted.isEmpty()) null else converted.toMap()

                return ffa.addLexicalLayer(groupNode, layer.partTypeName, lData.lexicon, map)
            }

            fun loadCascadingLayer( lData: SifAnimFfaLayer_Cascading, layer: SifAnimFfaLayer) : IFfaLayer? {
                val groupNode = context.nodes.getOrNull(lData.groupedNodeId) as? GroupNode ?: return null

                val subContracts = lData.sublayers.mapNotNull {
                    val infoGroup = context.nodes.getOrNull(it.nodeId)  as? GroupNode ?: return@mapNotNull null
                    FfaCascadingSublayerContract(infoGroup, it.lexicalKey, it.primaryLen, it.lexicon)
                }

                return ffa.addCascadingLayer(groupNode, layer.partTypeName, subContracts, lData.lexicon)
            }

            for (fLayer in data.layers) {
                val layer = when( val lData = fLayer.data) {
                    is SifAnimFfaLayer_Grouped -> loadGroupedLayer(lData, fLayer)
                    is SifAnimFfaLayer_Lexical -> loadLexicalLayer(lData, fLayer)
                    is SifAnimFfaLayer_Cascading -> loadCascadingLayer(lData, fLayer)
                } ?: continue

                layer.asynchronous = fLayer.isAsync
            }

            // Add
            context.animations.add(ffa)
            workspace.animationManager.addAnimation(ffa)
            val stateBind = workspace.animationStateSvc.getState(ffa)
            stateBind.speed = animation.speed
            stateBind.zoom = animation.zoom.i
        }
    }

    fun importAnsp(workspace: MImageWorkspace, ansp: SifAnspChunk, context: ImportContext) {
        fun getFfa( animId: Int) =
            context.animations.getOrNull(animId) as? FixedFrameAnimation

        for (fSpace in ansp.spaces) {
            val space = FFAAnimationSpace(fSpace.name, workspace)

            for (fAnim in fSpace.anims) {
                val anim = getFfa(fAnim.animId) ?: continue
                space.addAnimation(anim)
                val endLinkAnim = getFfa(fAnim.endLinkAnimId)
                if( endLinkAnim != null)
                    space.setOnEndBehavior(anim, Pair( endLinkAnim, fAnim.onEndFrame ?: 0))

                space.stateView.setLogicalSpace(anim, Vec2i(fAnim.logicalX, fAnim.logicalY))
            }

            for (fLink in fSpace.links) {
                val orig = getFfa(fLink.origAnimId) ?: continue
                val dest = getFfa(fLink.destAnimId) ?: continue
                space.addLink(FFAAnimationSpace.SpacialLink(orig, fLink.origFrame, dest, fLink.destFrame))
            }

            workspace.animationSpaceManager.addAnimationSpace(space, true)
        }
    }

    fun importPltt(workspace: MImageWorkspace, pltt: SifPlttChunk, context: ImportContext) {
        for (palette in pltt.palettes)
            workspace.paletteSet.addPalette(palette.name, false, palette.raw)

        // Remove Default Palette (only if any loaded exist)
        if( pltt.palettes.any())
            workspace.paletteSet.removePalette(0)
    }

    fun importTplt(workspace: MImageWorkspace, tplt: SifTpltChunk, context: ImportContext) {
        val nodeMap = tplt.nodeMaps.mapNotNull { entry->
            val node = context.nodes.getOrNull(entry.nodeId) ?: return@mapNotNull null
            val colors = entry.belt.map { ColorARGB32Normal(it) }
            Pair(node, colors)
        }.toMap()

        val spriteMap = tplt.spritePartMaps.mapNotNull { entry ->
            val group = context.nodes.getOrNull(entry.groupNodeId) as? GroupNode ?: return@mapNotNull null
            val colors = entry.belt.map { ColorARGB32Normal(it) }
            Pair(Pair(group, entry.partName), colors)
        }.toMap()

        workspace.paletteMediumMap.import(nodeMap, spriteMap)
    }

    fun importView(workspace: MImageWorkspace, viewChunk: SifViewChunk, context: ImportContext) {
        workspace.viewSystem.numActiveViews = viewChunk.views.count()

        viewChunk.views.forEachIndexed { viewId, fView ->
            val selected = context.nodes.getOrNull(fView.selectedNodeId)
            workspace.viewSystem.setCurrentNode(viewId, selected)

            fView.nodeProperties.forEachIndexed { index, properties ->
                val node = context.nodes[index]

                val visible = (properties.bitmap.i == 1)

                val render = RenderMethod(
                    RenderMethodType.values()[properties.renderMethod],
                    properties.renderValue )
                val props = properties.run { NodeViewProperties(ox, oy, visible, alpha, render )}

                workspace.viewSystem.set(node, props, viewId)
            }
        }
    }
}