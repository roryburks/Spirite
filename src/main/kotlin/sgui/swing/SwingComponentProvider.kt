package sgui.swing

import sgui.generic.components.*
import sgui.swing.components.SwGradientSlider
import sgui.swing.components.*
import rb.glow.IImage
import sgui.generic.Orientation
import sgui.generic.components.ITreeView
import sgui.swing.advancedComponents.SwTreeView
import sgui.generic.components.crossContainer.CrossInitializer
import rb.glow.color.SColor
import sgui.generic.components.crossContainer.ICrossPanel

object SwingComponentProvider : IComponentProvider {

    override fun <T> BoxList(boxWidth: Int, boxHeight: Int, entries: Collection<T>? ): IBoxList<T>
        = SwBoxList(boxWidth, boxHeight, entries)

    override fun Button(str: String?) : IButton = SwButton(str)
    override fun CheckBox(): ICheckBox = SwCheckBox()
    override fun RadioButton(label: String, selected: Boolean): IRadioButton = SwRadioButton(label, selected)
    override fun GradientSlider(minValue: Float, maxValue: Float, label: String) : IGradientSlider = SwGradientSlider(minValue, maxValue, label)
    override fun Label(text: String): ILabel = SwLabel(text)
    override fun EditableLabel(text: String): IEditableLabel = SwEditableLabel(text)
    override fun ScrollBar(orientation: Orientation, context: IComponent, minScroll: Int, maxScroll: Int, startScroll: Int, scrollWidth: Int) : IScrollBar
        = SwScrollBar(orientation, context, minScroll, maxScroll, startScroll, scrollWidth)
    override fun ScrollContainer(component: IComponent) = SwScrollContainer(component)

    override fun ToggleButton(startChecked: Boolean): IToggleButton = SwToggleButton(startChecked)
    override fun CrossPanel(constructor: (CrossInitializer.()->Unit)?): ICrossPanel = SwPanel().apply { constructor?.also { setLayout(it) } }
    override fun TabbedPane(): ITabbedPane = SwTabbedPane()
    override fun <T> ComboBox(things: Array<T>): IComboBox<T> = SwComboBox(things)
    override fun <T> TreeView(): ITreeView<T> = SwTreeView()

    override fun TextField(): ITextField = SwTextField()
    override fun IntField(min: Int, max: Int, allowsNegative: Boolean): IIntField = SwIntField(min, max, allowsNegative)
    override fun FloatField( min: Float, max: Float, allowsNegative: Boolean): IFloatField = SwFloatField(min, max, allowsNegative)

    override fun TextArea(): ITextArea = SwTextArea()

    override fun Separator(orientation: Orientation): ISeparator = SwSeparator(orientation)
    override fun ColorSquare(color: SColor): IColorSquare = SwColorSquare(color)

    override fun Slider(min: Int, max: Int, value: Int): ISlider = SwSlider(min, max, value)

    override fun ImageBox(img: IImage?): IImageBox = SwImageBox(img)
}