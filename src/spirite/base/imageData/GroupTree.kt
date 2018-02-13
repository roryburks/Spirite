package spirite.base.imageData

import spirite.base.imageData.layers.Layer

class GroupTree {
    val root = GroupNode()

    abstract inner class Node {
        fun getLayerNodes() : List<LayerNode> {
            TODO()
        }
    }

    inner class LayerNode(
            val layer: Layer
    ) : Node() {

    }
    inner class GroupNode : Node() {

    }
}