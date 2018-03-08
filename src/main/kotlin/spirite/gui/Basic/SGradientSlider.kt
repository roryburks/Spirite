package spirite.gui.Basic

import spirite.base.util.InvertibleFunction
import spirite.base.util.MUtil
import spirite.base.util.delegates.OnChangeDelegate
import spirite.gui.Bindable
import spirite.gui.Skin
import spirite.gui.UIUtil
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.DecimalFormat
import javax.swing.JPanel


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

internal interface IGradientSliderNonUIImpl : IGradientSliderNonUI {
    var underlying :Float
    var underlyingMin :Float
    var underlyingMax :Float
}

private class SGradientSliderNonUI(
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

class SGradientSlider(
        minValue : Float = 0f,
        maxValue : Float = 1f,
        label: String = "")
    :JPanel(), IGradientSliderNonUIImpl by SGradientSliderNonUI(minValue, maxValue), IGradientSlider
{
    override var bgGradLeft: Color by UI(Skin.GradientSlider.BgGradLeft.color)
    override var bgGradRight: Color by UI(Skin.GradientSlider.BgGradRight.color)
    override var fgGradLeft: Color by UI(Skin.GradientSlider.FgGradLeft.color)
    override var fgGradRight: Color by UI(Skin.GradientSlider.FgGradRight.color)
    override var disabledGradLeft: Color by UI(Skin.GradientSlider.DisabledGradLeft.color)
    override var disabledGradRight: Color by UI(Skin.GradientSlider.DisabledGradRight.color)
    override var label : String by UI(label)

    init {
        Bindable(0f, {repaint()}).bind(valueBind)

        val adapter = object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if( isEnabled)
                    underlying = MUtil.lerp(minValue, maxValue, e.x / width.toFloat())
                super.mousePressed(e)
            }
            override fun mouseDragged(e: MouseEvent) {
                if( isEnabled)
                    underlying = MUtil.lerp(minValue, maxValue, e.x / width.toFloat())
                super.mouseDragged(e)
            }
        }
        addMouseListener(adapter)
        addMouseMotionListener(adapter)
    }

    private val valAsStr : String
        get() {
            val df = DecimalFormat()
            df.maximumFractionDigits = 2
            df.minimumFractionDigits = 2
            return df.format(value)
        }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D

        val oldP = g2.paint
        g2.paint = GradientPaint( 0f, 0f, bgGradLeft, width + 0f, 0f, bgGradRight)
        g2.fillRect( 0, 0, width, height)

        g2.paint = when( isEnabled) {
            true -> GradientPaint( 0f, 0f, fgGradLeft, 0f, height + 0f, fgGradRight)
            else -> GradientPaint( 0f, 0f, disabledGradLeft, 0f, height + 0f, disabledGradRight)
        }
        g2.fillRect( 0, 0, Math.round(width * (underlying - underlyingMin + 0f) / (underlyingMax - underlyingMin + 0f)), height)
        g2.color = Color(222,222,222)

        UIUtil.drawStringCenter( g2, label + valAsStr, getBounds())

        g2.paint = oldP
        g2.color = Color.BLACK
        g2.drawRect( 0, 0, width-1, height-1)
    }

    private inner class UI<T>( defaultValue: T) : OnChangeDelegate<T>( defaultValue, {repaint()})
}