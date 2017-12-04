package spirite.base.image_data.animations

import spirite.base.image_data.GroupTree

interface NodeLinkedAnimation {
    fun nodesChanged(changed: List<GroupTree.Node>)
}