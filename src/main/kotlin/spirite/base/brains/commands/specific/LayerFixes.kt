package spirite.base.brains.commands.specific

import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.mathUtil.f
import spirite.base.graphics.drawer.IImageDrawer
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpritePartStructure
import spirite.base.imageData.mediums.ArrangedMediumData

object LayerFixes {
    fun ApplyTransformAccrossNode(workspace: IImageWorkspace, node: Node, transform: ITransformF) {
        fun rec(node: Node) {
            if (node is LayerNode) {
                val layer = node.layer
                val drawers = layer.imageDependencies.map { it.medium.getImageDrawer(ArrangedMediumData(it)) }
                drawers
                        .filterIsInstance<IImageDrawer.ITransformModule>()
                        .forEach { it.transform(transform, false) }
            }
            if (node is GroupNode) {
                node.children.forEach { rec(it) }
            }
        }

        workspace.undoEngine.doAsAggregateAction("Mass Transform") {
            rec(node)
        }
    }

    fun bakeOffset(workspace: IImageWorkspace, node: LayerNode) {
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


    fun copyUnusedMediumsIntoSprite(workspace: MImageWorkspace) {
        val used = workspace.groupTree.root.imageDependencies
            .map { it.id }
            .toHashSet()

        val toRevive = workspace.mediumRepository.dataList
            .filter { !used.contains(it) }
            .mapIndexed { index, i ->
                Pair(MediumHandle(workspace, i), SpritePartStructure(index, "imp_$index"))
            }

        if( toRevive.any()) {
            val spriteLayer = SpriteLayer(workspace, toRevive)
            workspace.groupTree.importLayer(workspace.groupTree.root, "UnusedMediums", spriteLayer)
        }
    }
}