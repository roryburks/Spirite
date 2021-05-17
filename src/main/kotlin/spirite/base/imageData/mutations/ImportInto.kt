package spirite.base.imageData.mutations

import sgui.swing.hybrid.Transferables.ILayerBuilder
import spirite.base.imageData.MImageWorkspace


fun MImageWorkspace.ImportInto(layerBuilder: ILayerBuilder) {
    groupTree.apply { importLayer(selectedNode, "ImportedLayer", layerBuilder.buildLayer(this@ImportInto)) }
}