package spirite.base.graphics.isolation

import rb.glow.gle.RenderRubric
import spirite.base.imageData.groupTree.Node

object NilNodeIsolator : IIsolator {
    override fun getIsolatorForNode(node: Node) = this
    override val isDrawn: Boolean = false
    override val rubric: RenderRubric? = null
}