package spirite.base.imageData.animation

import spirite.base.util.binding.CruddyBindable
import rb.vectrix.mathUtil.f

class AnimationState(
    met: Float = 0f,
    zoom: Int = 2,
    speed: Float = 8f)
{
    val metBind = CruddyBindable(met)
    var met by metBind

    val zoomBind = CruddyBindable(zoom)
    var zoom by zoomBind
    val zoomF : Float get() = if( zoom > 0) zoom.f else 1.f / (2.f-zoom.f)

    val speedBind = CruddyBindable(speed)
    var speed by speedBind
}