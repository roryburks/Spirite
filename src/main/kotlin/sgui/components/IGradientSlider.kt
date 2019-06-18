package sgui.components

import rb.glow.color.Color
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.vectrix.functions.InvertibleFunction
import rb.vectrix.mathUtil.MathUtil


interface IGradientSliderNonUI {
    var value: Float
    val valueBind: Bindable<Float>
    var mutatorPositionToValue: InvertibleFunction<Float>?
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
            val to = MathUtil.clip( minValue, to, maxValue)
            underlying = mutatorPositionToValue?.invert(to) ?: to
        }
    override val valueBind = Bindable(maxValue)
            .also { it.addObserver { new, _ -> _underlying = mutatorPositionToValue?.invert(new) ?: new } }

    override var mutatorPositionToValue: InvertibleFunction<Float>? = null
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
            underlyingMin = mutatorPositionToValue?.invert(to) ?: to
        }
    override var maxValue = maxValue
        set(to) {
            field = to
            if( value > to)
                this.value = to
            underlyingMax = mutatorPositionToValue?.invert(to) ?: to
        }

    override var underlying
        get() = _underlying
        set(to) {
            val to = MathUtil.clip(underlyingMin, to, underlyingMax)
            valueBind.field = mutatorPositionToValue?.perform(to) ?: to
            _underlying = to
        }
    private var _underlying : Float = maxValue
    override var underlyingMin = minValue
    override var underlyingMax = maxValue
}