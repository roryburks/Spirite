package spirite.base.image_data.animations.ffa

import spirite.base.graphics.RenderProperties
import spirite.base.graphics.renderer.RenderEngine.RenderMethod
import spirite.base.graphics.renderer.RenderEngine.TransformedHandle
import spirite.base.image_data.AnimationManager
import spirite.base.image_data.AnimationState
import spirite.base.image_data.animations.ffa.FFALayer.FFAFrame
import spirite.base.util.MUtil
import java.util.HashMap
import kotlin.collections.ArrayList


class FFAAnimationState(
        context: AnimationManager,
        val anim : FixedFrameAnimation
) : AnimationState(context, anim )
{
    var selectedMet: Int = 0
        set(value) {
            field = value
            triggerChange()
        }
    private val byTick = HashMap<Int, RenderProperties>()
    private val defaultProperties = RenderProperties()
    var selectedFrame : FFAFrame? = null
        set(value)  {
            if( value?.node != null) {
                anim.context.selectedNode = value.node
            }
            field = value
        }

    init {
        defaultProperties.isVisible = false
        resetSubstatesForTicks()
    }

    fun resetSubstatesForTicks() {
        byTick.clear()
        byTick.put( 0, RenderProperties(trigger))
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
    fun canonizeRelTick(t: Int, center: Int? = null): Int {
        val _center = center ?: selectedMet
        val L = anim.end - anim.start
        return ((t - _center) % L + L + L / 2) % L - L / 2
    }

    override fun buildDrawTable(): List<List<TransformedHandle>> {
        val table = ArrayList<List<TransformedHandle>>()

        val T = selectedMet
        val L = anim.end - anim.start + 1

        for( i in -(L-1)/2..L/2) {
            val properties = getSubstateForRelativeTick(i)
            if( !properties.isVisible)
                continue

            val drawList = anim.getDrawList( MUtil.cycle(anim.start, anim.end, i+T).toFloat())
            for( tr in drawList) {
                tr.alpha = properties.alpha
                tr.method = properties.method
                tr.renderValue = properties.renderValue
            }

            table.add(drawList)
        }

        return table
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