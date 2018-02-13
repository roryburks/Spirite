package spirite.base.imageData

import spirite.base.imageData.layers.Layer

/**
 * The GroupTree is an abstract Container Structure used by several components to organize ImageData in a hierarchical
 * tree structure.
 */
class GroupTree(
        val contex: IImageWorkspace
) {
    val root = GroupNode(null, "ROOT")

    abstract inner class Node(
            val parent: Node?,
            val name: String
    ) {
        val children: List<Node> get() = _children
        private val _children = mutableListOf<Node>()

        fun getLayerNodes() : List<LayerNode> {
            TODO()
        }
    }

    inner class LayerNode(
            parent: Node?,
            name: String,
            val layer: Layer
    ) : Node(parent, name) {

    }
    inner class GroupNode(
            parent: Node?,
            name: String) : Node(parent, name) {

    }
}