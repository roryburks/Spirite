package spirite.base.image_data.animations.ffa

import spirite.base.image_data.AnimationManager
import spirite.base.image_data.AnimationState
import java.util.HashMap
import spirite.base.graphics.RenderProperties
import spirite.base.graphics.renderer.RenderEngine.RenderMethod
import spirite.base.image_data.animations.ffa.FFALayer.FFAFrame


class FFAAnimationState(
        context: AnimationManager,
        val anim : FixedFrameAnimation
) : AnimationState(context, anim )
{
    var selectedMet: Int = 0
    private val byTick = HashMap<Int, RenderProperties>()
    private val defaultProperties = RenderProperties()

    var selectedFrame : FFAFrame? = null
        set(value)  {
            if( value?.node != null) {
                anim.context.selectedNode = value.node
            }
            field = value
        }

    fun getSubstateForRelativeTick(tick: Int): RenderProperties {
        val ss = byTick[tick]
        return RenderProperties(ss ?: defaultProperties)
    }
    fun hasSubstateForRelativeTick(tick: Int): Boolean {
        return byTick[tick] != null
    }

    fun putSubstateForRelativeTick(tick: Int, properties: RenderProperties) {
        byTick.remove(tick)
        byTick.put(tick, RenderProperties(properties, trigger))
        triggerChange()
    }
    fun cannonizeRelTick(t: Int, center: Int? = null): Int {
        val _center = center ?: selectedMet
        val L = anim.end - anim.start
        return ((t - _center) % L + L + L / 2) % L - L / 2
    }

    private val trigger = object : RenderProperties.Trigger {
        override fun visibilityChanged(newVisible: Boolean): Boolean {
            triggerChange()
            return true
        }
        override fun alphaChanged(newAlpha: Float): Boolean {
            triggerChange()
            return true
        }
        override fun methodChanged(newMethod: RenderMethod, newValue: Int): Boolean {
            triggerChange()
            return true
        }
    }
}