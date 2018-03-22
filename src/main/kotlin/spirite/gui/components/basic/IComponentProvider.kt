package spirite.gui.components.basic

import spirite.base.util.Colors
import spirite.gui.Orientation
import spirite.gui.components.advanced.ITreeView
import spirite.pc.gui.SColor
import kotlin.Int.Companion

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
    fun TabbedPane( ): ITabbedPane
    fun <T> ComboBox( things: Array<T>) : IComboBox<T>
    fun <T> TreeView() : ITreeView<T>
    fun TextField() : ITextField
    fun IntField(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE, allowsNegative: Boolean = false) : IIntField
    fun FloatField(min: Float = Float.MIN_VALUE, max: Float = Float.MAX_VALUE, allowsNegative: Boolean = false) : IFloatField

    fun Separator( orientation: Orientation) : ISeparator
    fun ColorSquare( color: SColor = Colors.BLACK) : IColorSquare

    fun <T> BoxList(boxWidth: Int, boxHeight: Int, entries: Collection<T>? = null) : IBoxList<T>
}