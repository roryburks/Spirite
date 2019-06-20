package spirite.base.brains.commands.specific

import kotlinx.coroutines.handleExceptionViaHandler
import rb.vectrix.linear.ITransform
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.mathUtil.f
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.groupTree.GroupTree
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.mediums.ArrangedMediumData

object LayerFixes {
    fun ApplyTransformAccrossNode(workspace: IImageWorkspace, node: GroupTree.Node, transform: ITransformF) {
        fun rec(node: GroupTree.Node) {
            if (node is GroupTree.LayerNode) {
                val layer = node.layer
                val drawers = layer.imageDependencies.map { it.medium.getImageDrawer(ArrangedMediumData(it)) }
                drawers
                        .filterIsInstance<IImageDrawer.ITransformModule>()
                        .forEach { it.transform(transform, false) }
            }
            if (node is GroupTree.GroupNode) {
                node.children.forEach { rec(it) }
            }
        }

        workspace.undoEngine.doAsAggregateAction("Mass Scaline") {
            rec(node)
        }
    }

    fun bakeOffset(workspace: IImageWorkspace, node: GroupTree.LayerNode) {
        val layer = node.layer
        val oldProperties = workspace.viewSystem.get(node)
        val ox = workspace.viewSystem.get(node).ox
        val oy = workspace.viewSystem.get(node).oy
        val newProperties = oldProperties.copy(ox = 0, oy = 0)
        val trans = ImmutableTransformF.Translation(ox.f, oy.f)

        workspace.undoEngine.doAsAggregateAction("Bake Offset into Layer") {
            workspace.viewSystem.set(node, newProperties)
            layer.imageDependencies.forEach {
                (layer.getDrawer(ArrangedMediumData(it)) as? IImageDrawer.ITransformModule)?.transform(trans)
            }
        }
    }

    fun MoveSpritePart(part: SpriteLayer.SpritePart, newLayer: SpriteLayer?) {

    }
}