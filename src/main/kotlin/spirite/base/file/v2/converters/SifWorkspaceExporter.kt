package spirite.base.file.v2.converters

import spirite.base.file.SaveLoadUtil
import spirite.base.file.save.MediumPreparer
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.core.file.SifConstants
import spirite.core.file.SifFileException
import spirite.core.file.contracts.*

interface ISifWorkspaceExporter {
    fun export( workspace: IImageWorkspace) : SifFile
}

class SifWorkspaceExporter : ISifWorkspaceExporter{
    class ExportContext(
        val workspace: IImageWorkspace )
    {
        val nodeMap = mutableMapOf<Node,Int>()
        val animMap = mutableMapOf<Animation, Int>()

        val root = workspace.groupTree.root
        val floatingData = workspace.mediumRepository.dataList
            .mapNotNull { workspace.mediumRepository.floatData(it) { medium -> MediumPreparer.prepare(medium) } }

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

    fun exportImgd(context: ExportContext) : SifImgdChunk { TODO() }

    fun exportAnim(context: ExportContext): SifAnimChunk { TODO() }

    fun exportPltt(context: ExportContext): SifPlttChunk { TODO()}

    fun exportTpltChunk(context: ExportContext) : SifTpltChunk { TODO()}

    fun exportAnspChunk(context: ExportContext) : SifAnspChunk { TODO()}

    fun exportViewChunk(context: ExportContext) : SifViewChunk { TODO()}

}