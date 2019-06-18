package sguiSwing.components

import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import sgui.components.IComponent
import sgui.components.ISlider
import sguiSwing.SwingComponentProvider
import sguiSwing.skin.Skin.BevelBorder.*
import sguiSwing.skin.Skin.Global.*
import java.awt.*
import java.util.*
import javax.swing.JSlider
import javax.swing.plaf.basic.BasicSliderUI

class SwSlider
private constructor(private val imp : SwSliderImp)
    : ISlider, IComponent by SwComponent(imp)
{
    constructor(min: Int, max:Int, value: Int) : this(SwSliderImp(min, max, value))

    private var locked = false

    override var min: Int
        get() = imp.minimum
        set(value) {imp.minimum = value}
    override var max: Int
        get() = imp.maximum
        set(value) {imp.maximum = value}
    override val valueBind = Bindable(imp.value)
            .also { it.addObserver { new, _ ->
                if (!locked) {
                    locked = true; imp.value = new; locked = false
                }
            }}
    override var value by valueBind

    // region UI Piping
    override var tickSpacing: Int
        get() = imp.majorTickSpacing
        set(value) {imp.majorTickSpacing = value}

    override fun setLabels(labels: Map<Int, String>) {
        val dictionary = Hashtable<Int, Component>(labels.count())
        labels.forEach { key, value -> dictionary[key] = SwingComponentProvider.Label(value).jcomponent }
        imp.labelTable = dictionary
    }

    override var snapsToTick: Boolean
        get() = imp.snapToTicks
        set(value) {imp.snapToTicks = value}
    // endregion

    init {
        background = Bg.scolor
        foreground = Fg.scolor

        imp.addChangeListener { if(!locked){locked = true; value = imp.value ; locked = false}}
    }

    protected class SwSliderImp(min: Int, max: Int, value: Int) : JSlider(min, max, value) {
        init {
            this.setUI(UI(this))

            paintLabels = true
            paintTicks = true
            snapToTicks = true
        }

        private inner class UI(context : JSlider) : BasicSliderUI( context) {
            override fun scrollDueToClickInTrack(dir: Int) {
                var value = slider.value

                if (slider.orientation == JSlider.HORIZONTAL) {
                    value = this.valueForXPosition(slider.mousePosition.x)
                } else if (slider.orientation == JSlider.VERTICAL) {
                    value = this.valueForYPosition(slider.mousePosition.y)
                }
                slider.value = value
            }

            override fun paintTrack(g: Graphics) {
                val yc = trackRect.y + trackRect.height / 2

                val x1 = trackRect.x
                val x2 = trackRect.x + trackRect.width
                val y1 = yc - 2
                val y2 = yc + 2

                val g2 = g.create() as Graphics2D
                g2.color = Med.jcolor
                g2.fillRect(x1, y1, x2 - x1 - 1, y2 - y1 - 1)

                g2.color = Light.jcolor
                g2.drawLine(x1, y1, x2, y1)
                g2.drawLine(x1, y1, x1, y2)

                g2.color = Darker.jcolor
                g2.drawLine(x1 + 1, y1 + 1, x2 - 1, y1 + 1)
                g2.drawLine(x1 + 1, y1 + 1, x1 + 1, y2 - 1)

                g2.color = Dark.jcolor
                g2.drawLine(x1, y2, x2, y2)
                g2.drawLine(x2, y1, x2, y2)

                g2.dispose()
            }

            override fun paintThumb(g: Graphics) {

                val cx = thumbRect.x + thumbRect.width / 2

                val g2 = g.create() as Graphics2D


                val r: Rectangle
                if (height - 1 < thumbRect.height)
                    r = Rectangle(thumbRect.x, 0, thumbRect.width, height - 1)
                else
                    r = thumbRect

                val w = 2
                val x1l = r.x
                val x2l = r.x + r.width / 2 - 2
                val x1r = r.x + r.width - (r.width / 2 - 2) - 1
                val x2r = r.x + r.width - 1
                val y1 = r.y
                val y2 = r.y + r.height

                val leftx = intArrayOf(x1l, x2l, x2l, x1l + w, x1l + w, x2l, x2l, x1l)
                val lefty = intArrayOf(y1, y1, y1 + w, y1 + w, y2 - w, y2 - w, y2, y2)
                val rightx = intArrayOf(x1r, x2r, x2r, x1r, x1r, x2r - w, x2r - w, x1r)
                val righty = intArrayOf(y1, y1, y2, y2, y2 - w, y2 - w, y1 + w, y1 + w, y1)

                // Main Part
                val fgD = Med.jcolor
                val fgL = FgLight.jcolor
                val fg = Fg.jcolor
                g2.color = fg
                g2.fillPolygon(leftx, lefty, leftx.size)
                g2.fillPolygon(rightx, righty, rightx.size)

                // Highlights
                g2.color = fgL
                g2.drawLine(x1l, y1, x2l, y1)
                g2.drawLine(x1l, y1, x1l, y2)
                g2.drawLine(x1r, y1, x1r, y1 + w)
                g2.drawLine(x1r, y1, x2r, y1)
                g2.drawLine(x1r, y2, x2r, y2)
                g2.drawLine(x1r, y2, x1r, y2 - w)

                // Shadows
                g2.color = fgD
                g2.drawLine(x2l, y1, x2l, y1 + w)
                g2.drawLine(x1l + w, y1 + w, x2l, y1 + w)
                g2.drawLine(x1l, y2, x2l, y2)
                g2.drawLine(x2l, y2, x2l, y2 - w)
                g2.drawLine(x1r, y2, x2r, y2)
                g2.drawLine(x2r, y1, x2r, y2)

                // Center Line
                g2.color = Color.RED
                g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
                g2.drawLine(cx, thumbRect.y, cx, thumbRect.y + thumbRect.height)
                g2.dispose()

                //super.paintThumb(g);
            }
        }
    }
}