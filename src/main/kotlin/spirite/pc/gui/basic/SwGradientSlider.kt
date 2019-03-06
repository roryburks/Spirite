package spirite.pc.gui.basic

import rb.extendo.delegates.OnChangeDelegate
import rb.owl.bindable.addObserver
import rb.vectrix.mathUtil.MathUtil
import spirite.gui.UIUtil
import spirite.gui.components.basic.GradientSliderNonUI
import spirite.gui.components.basic.IGradientSlider
import spirite.gui.components.basic.IGradientSliderNonUIImpl
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.resources.Skin
import spirite.pc.gui.adaptMouseSystem
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.text.DecimalFormat


class SwGradientSlider
private constructor(minValue: Float, maxValue: Float, label: String, val imp : SwGradientSliderImp)
    :
        IGradientSliderNonUIImpl by GradientSliderNonUI(minValue, maxValue),
        IGradientSlider,
        ISwComponent by SwComponent(imp)
{
    init {
        imp.context = this
    }

    constructor(
            minValue : Float = 0f,
            maxValue : Float = 1f,
            label: String = "") : this( minValue, maxValue, label, SwGradientSliderImp())

    override var bgGradLeft: Color by UI(Skin.GradientSlider.BgGradLeft.jcolor)
    override var bgGradRight: Color by UI(Skin.GradientSlider.BgGradRight.jcolor)
    override var fgGradLeft: Color by UI(Skin.GradientSlider.FgGradLeft.jcolor)
    override var fgGradRight: Color by UI(Skin.GradientSlider.FgGradRight.jcolor)
    override var disabledGradLeft: Color by UI(Skin.GradientSlider.DisabledGradLeft.jcolor)
    override var disabledGradRight: Color by UI(Skin.GradientSlider.DisabledGradRight.jcolor)
    override var label : String by UI(label)

    private class SwGradientSliderImp() : SJPanel() {
        init {adaptMouseSystem()}
        var context : SwGradientSlider? = null

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            val c = context ?: return

            val g2 = g as Graphics2D

            val oldP = g2.paint
            g2.paint = GradientPaint( 0f, 0f, c.bgGradLeft, width + 0f, 0f, c.bgGradRight)
            g2.fillRect( 0, 0, width, height)

            g2.paint = when( isEnabled) {
                true -> GradientPaint( 0f, 0f, c.fgGradLeft, 0f, height + 0f, c.fgGradRight)
                else -> GradientPaint( 0f, 0f, c.disabledGradLeft, 0f, height + 0f, c.disabledGradRight)
            }
            g2.fillRect( 0, 0, Math.round(width * (c.underlying - c.underlyingMin + 0f) / (c.underlyingMax - c.underlyingMin + 0f)), height)
            g2.color = Color(222,222,222)

            UIUtil.drawStringCenter( g2, c.label + c.valAsStr, getBounds())

            g2.paint = oldP
            g2.color = Color.BLACK
            g2.drawRect( 0, 0, width-1, height-1)
        }
    }

    init {
        valueBind.addObserver { _, _ ->  redraw()}

        val trigger : (MouseEvent) -> Unit = {
            if( imp.isEnabled)
                underlying = MathUtil.lerp(underlyingMin, underlyingMax, it.point.x / imp.width.toFloat())
        }
        onMousePress += trigger
        onMouseDrag += trigger
    }

    private val valAsStr : String
        get() {
            val df = DecimalFormat()
            df.maximumFractionDigits = 2
            df.minimumFractionDigits = 2
            return df.format(value)
        }


    private inner class UI<T>( defaultValue: T) : OnChangeDelegate<T>( defaultValue, {redraw()})
}