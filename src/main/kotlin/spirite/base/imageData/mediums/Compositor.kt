package spirite.base.imageData.mediums

import rb.glow.GraphicsContext_old
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.Node


interface ICompositeSource {
    fun appliedToMedium(medium: MediumHandle) = false
    fun appliedToNode( node: Node) = false
    val drawsSource : Boolean
    val drawer: (GraphicsContext_old) -> Unit
}

data class HandleCompositeSource(
        val arranged: ArrangedMediumData,
        override val drawsSource : Boolean = true,
        override val drawer : (GraphicsContext_old) -> Unit) : ICompositeSource
{
    override fun appliedToMedium(medium: MediumHandle) = arranged.handle == medium
}

data class NodeCompositeSource(
        val node: Node,
        override val drawsSource : Boolean = true,
        override val drawer : (GraphicsContext_old) -> Unit) : ICompositeSource
{
    override fun appliedToNode(node: Node) = node == this.node
}


class Compositor {
    var compositeSource : HandleCompositeSource? = null
        set(value) {
            field = value
            triggerCompositeChanged()
        }

    fun triggerCompositeChanged() {
        compositeSource?.arranged?.handle?.refresh()
    }
}