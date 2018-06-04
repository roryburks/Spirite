package spirite.base.imageData.animation

import spirite.base.brains.Bindable
import spirite.base.util.f

class AnimationState(
    met: Float = 0f,
    zoom: Int = 2,
    speed: Float = 8f)
{
    val metBind = Bindable(met)
    var met by metBind

    val zoomBind = Bindable(zoom)
    var zoom by zoomBind
    val zoomF : Float = if( zoom > 0) zoom.f else 1 / (1+zoom.f)

    val speedBind = Bindable(speed)
    var speed by speedBind
}