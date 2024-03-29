package spirite.base.file.aaf.export

import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.layers.sprite.SpriteLayer

object MediumNameMapper {
    fun getMap(workspace: IImageWorkspace) : Map<Int, String> {
        return workspace.groupTree
                .root.getAllAncestors()
                .filterIsInstance<LayerNode>()
                .map { it.layer }
                .filterIsInstance<SpriteLayer>()
                .flatMap { sprite -> sprite.parts.map{ Pair(it.handle.id, it.partName)} }
                .toMap()
    }

}

