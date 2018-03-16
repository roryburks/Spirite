package spirite.gui.components.basic

import spirite.gui.Orientation

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
    fun TabbedPane(): ITabbedPane
}