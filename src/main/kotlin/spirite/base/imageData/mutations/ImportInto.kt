package spirite.base.imageData.mutations

import spirite.base.imageData.MImageWorkspace
import spirite.sguiHybrid.transferables.ILayerBuilder


fun MImageWorkspace.importInto(layerBuilder: ILayerBuilder) {
    groupTree.apply { importLayer(selectedNode, "${layerBuilder.name} [imported]", layerBuilder.buildLayer(this@importInto)) }
}