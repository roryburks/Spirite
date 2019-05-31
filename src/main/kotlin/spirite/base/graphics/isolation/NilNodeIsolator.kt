package spirite.base.graphics.isolation

import spirite.base.graphics.RenderRubric
import spirite.base.imageData.groupTree.GroupTree.Node

object NilNodeIsolator : IIsolator {
    override fun getIsolatorForNode(node: Node) = this
    override val isDrawn: Boolean = false
    override val rubric: RenderRubric? = null
}