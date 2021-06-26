package spirite.base.imageData.animation.ffa

import spirite.base.imageData.groupTree.Node

data class FfaFrameStructure(
    val node: Node?,
    val marker: Marker,
    val length: Int
) {
    enum class Marker constructor(val fileId: Int){
        FRAME(1),
        START_LOCAL_LOOP(2),
        END_LOCAL_LOOP(4),
        GAP(3)
    }
}