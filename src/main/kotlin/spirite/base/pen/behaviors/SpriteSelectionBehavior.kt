package spirite.base.pen.behaviors

import rb.extendo.dataStructures.SinglySequence
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.Vec2i
import rb.vectrix.linear.invertN
import rb.vectrix.mathUtil.floor
import spirite.base.brains.toolset.WorkspaceScope
import spirite.base.brains.toolset.WorkspaceScope.*
import spirite.base.imageData.groupTree.GroupTree
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.groupTree.traverse
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.pen.Penner


class SpriteSelectionBehavior(
        penner: Penner,
        private val scope: WorkspaceScope)
    : PennerBehavior(penner)
{
    override fun onStart() {check()}
    override fun onTock() {}
    override fun onMove() {check()}

    private fun check() {
        val x = penner.x
        val y = penner.y
        val groupTree : GroupTree = penner.workspace?.groupTree ?: return
        val selectedNode = groupTree.selectedNode
        val validSpriteLayers : Sequence<GroupTree.Node> = when(scope) {
            Node -> (selectedNode)?.run { SinglySequence(this) } ?: emptySequence()
            Group -> {
                val group = (selectedNode as? GroupNode) ?: selectedNode?.parent ?: return
                group.traverse { it.isVisible }
            }
            Workspace -> {
                val root = groupTree.root
                root.traverse { it.isVisible }
            }
        }

        val res = validSpriteLayers
                .filterIsInstance<LayerNode>()
                .mapNotNull {node ->
                    val spriteLayer = node.layer as? SpriteLayer ?: return@mapNotNull null
                    checkRigPart(x, y , spriteLayer, node.tNodeToRoot.invertN())?.run { Pair(node, this) }
                }
                .maxBy { it.second.depth}

        if( res != null) {
            groupTree.selectedNode = res.first
            val part = res.second
            part.context.activePart = part
        }

    }

    private fun checkRigPart(x: Int, y: Int, layer: SpriteLayer, tWsToLayer: ITransform) : SpritePart? {
        return layer.parts.asReversed().asSequence()
                .filter { it.isVisible }
                .firstOrNull {
                    val local = ( tWsToLayer * it.tWholeToPart).apply(Vec2i(x,y))
                    val color = it.handle.medium.getColor(local.x.floor, local.y.floor)
                    color.alpha > 0.5f
                }
    }

}