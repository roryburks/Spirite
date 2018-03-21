package spirite.pc.gui

import spirite.gui.Orientation
import spirite.gui.components.advanced.ITreeView
import spirite.gui.components.advanced.SwTreeView
import spirite.gui.components.basic.*
import spirite.pc.gui.basic.*

object SwingComponentProvider : IComponentProvider {

    override fun Button(str: String?) : IButton = SwButton(str)
    override fun GradientSlider(minValue: Float, maxValue: Float, label: String) : IGradientSlider = SwGradientSlider(minValue, maxValue, label)
    override fun Label(text: String): ILabel = SwLabel(text)
    override fun ScrollBar(orientation: Orientation, context: IComponent, minScroll: Int, maxScroll: Int, startScroll: Int, scrollWidth: Int) : IScrollBar
        = SwScrollBar(orientation, context, minScroll, maxScroll, startScroll, scrollWidth)

    override fun ToggleButton(startChecked: Boolean): IToggleButton = SwToggleButton(startChecked)
    override fun CrossPanel(): ICrossPanel = SwPanel()
    override fun TabbedPane(): ITabbedPane = SwTabbedPane()
    override fun <T> ComboBox(things: Array<T>): IComboBox<T> = SwComboBox(things)
    override fun <T> TreeView(): ITreeView<T> = SwTreeView()

    override fun TextField(): ITextField = SwTextField()
    override fun IntField(min: Int, max: Int, allowsNegative: Boolean): IIntField = SwIntField(min, max, allowsNegative)
    override fun FloatField( min: Float, max: Float, allowsNegative: Boolean): IFloatField = SwFloatField(min, max, allowsNegative)

    override fun Separator(orientation: Orientation): ISeparator = SwSeparator(orientation)
    override fun ColorSquare(color: SColor): IColorSquare = SwColorSquare(color)
}