package spirite.base.image_data.animations.ffa

import spirite.base.image_data.GroupTree

open class FFAFrameAbstract(
        node : GroupTree.Node?,
        marker: Marker,
        length : Int,
        gapBefore : Int,
        gapAfter: Int
)
{
    val node = node ;
    val marker = marker ;
    var length = length ; internal  set
    var gapBefore = gapBefore ; internal  set
    var gapAfter = gapAfter ; internal  set

    enum class Marker {
        FRAME,
        START_LOCAL_LOOP,
        END_LOCAL_LOOP
    }
}