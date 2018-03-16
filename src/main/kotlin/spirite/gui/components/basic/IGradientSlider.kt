package spirite.gui.components.basic

import spirite.base.util.InvertibleFunction
import spirite.base.util.MUtil
import spirite.gui.Bindable
import java.awt.Color


interface IGradientSliderNonUI {
    var value: Float
    val valueBind: Bindable<Float>
    var mutator: InvertibleFunction<Float>?
    var minValue: Float
    var maxValue: Float
}

interface IGradientSlider : IGradientSliderNonUI, IComponent {
    var bgGradLeft : Color
    var bgGradRight : Color
    var fgGradLeft : Color
    var fgGradRight : Color
    var disabledGradLeft : Color
    var disabledGradRight : Color

    var label: String
}

interface IGradientSliderNonUIImpl : IGradientSliderNonUI {
    var underlying :Float
    var underlyingMin :Float
    var underlyingMax :Float
}

class GradientSliderNonUI(
        minValue : Float = 0f,
        maxValue : Float = 1f)
    : IGradientSliderNonUIImpl
{
    override var value : Float get() = valueBind.field
        set(to) {
            val to = MUtil.clip( minValue, to, maxValue)
            underlying = mutator?.invert(to) ?: to
        }
    override val valueBind = Bindable( maxValue, {_underlying = mutator?.invert(it) ?: it})
    override var mutator : InvertibleFunction<Float>? = null
        set(to) {
            field = to
            underlying = to?.invert(value) ?: value
            underlyingMin = to?.invert(minValue) ?: minValue
            underlyingMax = to?.invert(maxValue) ?: maxValue
        }

    override var minValue = minValue
        set(to) {
            field = to
            if( value < to)
                this.value = to
            underlyingMin = mutator?.invert(to) ?: to
        }
    override var maxValue = maxValue
        set(to) {
            field = to
            if( value > to)
                this.value = to
            underlyingMax = mutator?.invert(to) ?: to
        }

    override var underlying
        get() = _underlying
        set(to) {
            val to = MUtil.clip(underlyingMin, to, underlyingMax)
            valueBind.field = mutator?.perform(to) ?: to
            _underlying = to
        }
    private var _underlying : Float = maxValue
    override var underlyingMin = minValue
    override var underlyingMax = maxValue
}