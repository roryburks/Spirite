package spirite.base.imageData.mutations

import spirite.base.graphics.IImage
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.hybrid.Transferables.ILayerBuilder


fun MImageWorkspace.ImportInto(layerBuilder: ILayerBuilder) {
    groupTree.apply { importLayer(selectedNode, "ImportedLayer", layerBuilder.buildLayer(this@ImportInto)) }
}