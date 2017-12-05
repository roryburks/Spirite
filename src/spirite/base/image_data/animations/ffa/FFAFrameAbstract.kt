package spirite.base.image_data.animations.ffa

import spirite.base.image_data.GroupTree

data class FFAFrameStructure(
        val node : GroupTree.Node?,
        val marker: Marker,
        var length : Int,
        var gapBefore : Int = 0,
        var gapAfter: Int = 0
)
{
    enum class Marker {
        FRAME,
        START_LOCAL_LOOP,
        END_LOCAL_LOOP
    }
}