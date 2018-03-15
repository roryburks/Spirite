package spirite.pc.gui.basic

import spirite.gui.Orientation
import spirite.gui.basic.*

interface IComponentProvider {
    fun Button(str: String? = null) : IButton
    fun GradientSlider(
            minValue : Float = 0f,
            maxValue : Float = 1f,
            label: String = "") : IGradientSlider
    fun Label( text: String = "") : ILabel
    fun ScrollBar(
            orientation: Orientation,
            context: IComponent,
            minScroll: Int = 0,
            maxScroll: Int = 100,
            startScroll: Int = 0,
            scrollWidth : Int = 10) : IScrollBar
    fun ToggleButton(startChecked: Boolean = false) : IToggleButton
    fun CrossPanel() : ICrossPanel
}

object SwingComponentProvider : IComponentProvider{
    override fun Button(str: String?) : IButton = SwButton(str)
    override fun GradientSlider(minValue: Float, maxValue: Float, label: String) : IGradientSlider = SwGradientSlider(minValue, maxValue, label)
    override fun Label(text: String): ILabel = SwLabel(text)
    override fun ScrollBar(orientation: Orientation, context: IComponent, minScroll: Int, maxScroll: Int, startScroll: Int, scrollWidth: Int) : IScrollBar
        = SwScrollBar(orientation, context, minScroll, maxScroll, startScroll, scrollWidth)

    override fun ToggleButton(startChecked: Boolean): IToggleButton = SwToggleButton(startChecked)
    override fun CrossPanel(): ICrossPanel = SwPanel()
}