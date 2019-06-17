package spirite.base.brains.commands.specific

import rb.vectrix.linear.ITransformF
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

    fun MoveSpritePart(part: SpriteLayer.SpritePart, newLayer: SpriteLayer?) {

    }
}