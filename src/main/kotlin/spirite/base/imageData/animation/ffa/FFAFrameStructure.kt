package spirite.base.imageData.animation.ffa

import spirite.base.imageData.groupTree.GroupTree.Node

data class FfaFrameStructure(
        val node: Node?,
        val marker: Marker,
        val length: Int
) {
    enum class Marker {
        FRAME,
        START_LOCAL_LOOP,
        END_LOCAL_LOOP,
        GAP
    }
}