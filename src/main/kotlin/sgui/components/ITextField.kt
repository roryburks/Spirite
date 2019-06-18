package sgui.components

import rb.glow.color.Color
import rb.owl.bindable.Bindable
import rb.vectrix.mathUtil.MathUtil

interface ITextFieldNonUI {
    val textBind : Bindable<String>
    var text: String
}
class TextFieldNonUI : ITextFieldNonUI {
    override val textBind = Bindable("")
    override var text by textBind
}
interface ITextField : ITextFieldNonUI, IComponent {}


interface INumberFieldUI
{
    var validBg : Color
    var invalidBg : Color
}

interface IIntFieldNonUI
{
    val valueBind : Bindable<Int>
    var value : Int

    var min : Int
    var max : Int
}
interface IIntField : IIntFieldNonUI, IComponent
class IntFieldNonUI( min: Int, max: Int) : IIntFieldNonUI {
    override val valueBind = Bindable(0)
    override var value: Int
        get() = valueBind.field
        set(new) {
            val to = MathUtil.clip(min, new, max)
            valueBind.field = to
        }

    override var min: Int = min
        set(new) {
            field = new
            if(value < new) value = new
            if( max < min) max = min
        }
    override var max: Int = max
        set(new) {
            field = new
            if( value > new) value = new
            if( min > max) min = max
        }
}

interface IFloatFieldNonUI
{
    val valueBind : Bindable<Float>
    var value : Float

    var min : Float
    var max : Float
}
interface IFloatField : IFloatFieldNonUI, IComponent
class FloatFieldNonUI( min: Float, max: Float) : IFloatFieldNonUI {
    override val valueBind = Bindable(0f)
    override var value: Float
        get() = valueBind.field
        set(new) {
            val to = MathUtil.clip(min, new, max)
            valueBind.field = to
        }

    override var min: Float = min
        set(new) {
            field = new
            if(value < new) value = new
            if( max < min) max = min
        }
    override var max: Float = max
        set(new) {
            field = new
            if( value > new) value = new
            if( min > max) min = max
        }
}


