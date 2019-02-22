package spirite.base.imageData.animation

import rb.owl.bindable.Bindable
import rb.vectrix.mathUtil.f

class AnimationState(
    met: Float = 0f,
    zoom: Int = 2,
    speed: Float = 8f)
{

    val metBind = Bindable(met)
    val zoomBind = Bindable(zoom)
    val offsetXBind = Bindable(0)
    val offsetYBind = Bindable(0)
    val speedBind = Bindable(speed)

    var met by metBind
    var zoom by zoomBind
    var offsetX by offsetXBind
    var offsetY by offsetYBind
    var speed by speedBind

    val zoomF : Float get() = if( zoom > 0) zoom.f else 1.f / (2.f-zoom.f)
}