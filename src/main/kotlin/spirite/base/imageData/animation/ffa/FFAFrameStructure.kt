package spirite.base.imageData.animation.ffa

import spirite.base.imageData.groupTree.GroupTree.Node

data class FFAFrameStructure(
        val node: Node?,
        val marker: Marker,
        val length: Int,
        val gapBefore: Int = 0,
        val gapAfter: Int = 0
) {
    enum class Marker {
        FRAME,
        START_LOCAL_LOOP,
        END_LOCAL_LOOP
    }
}