package spirite.base.imageData.animation.services

import rb.owl.bindable.Bindable
import rb.vectrix.mathUtil.f

class AnimationStateBind(
    met: Float = 0f,
    zoom: Int = 2,
    speed: Float = 8f,
    offsetX: Int = 0,
    offsetY: Int = 0)
{

    val metBind = Bindable(met)
    val zoomBind = Bindable(zoom)
    val offsetXBind = Bindable(offsetX)
    val offsetYBind = Bindable(offsetY)
    val speedBind = Bindable(speed)

    var met by metBind
    var zoom by zoomBind
    var offsetX by offsetXBind
    var offsetY by offsetYBind
    var speed by speedBind

    val zoomF : Float get() = if( zoom > 0) zoom.f else 1.f / (2.f-zoom.f)

    fun toData() = AnimationStateData(met, zoom, offsetX, offsetY, speed)
    companion object {
        fun FromData(data: AnimationStateData) = data.run { AnimationStateBind(met, zoom, speed, offsetX, offsetY) }
    }
}

data class AnimationStateData(
    val met: Float,
    val zoom: Int,
    val offsetX : Int,
    val offsetY : Int,
    val speed: Float )

fun AnimationStateData.duplicateInto(bind: AnimationStateBind) {
    bind.met = met
    bind.zoom = zoom
    bind.offsetX = offsetX
    bind.offsetY = offsetY
    bind.speed = speed
}