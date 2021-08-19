package spirite.base.imageData.mutations

import spirite.base.imageData.MImageWorkspace
import spirite.sguiHybrid.transferables.ILayerBuilder


fun MImageWorkspace.ImportInto(layerBuilder: ILayerBuilder) {
    groupTree.apply { importLayer(selectedNode, "ImportedLayer", layerBuilder.buildLayer(this@ImportInto)) }
}