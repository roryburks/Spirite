package sgui.components

import rb.glow.Colors
import rb.glow.img.IImage
import rbJvm.glow.SColor
import sgui.components.crossContainer.CrossInitializer
import sgui.components.crossContainer.ICrossPanel

interface IComponentProvider {
    fun Button(str: String? = null) : IButton
    fun CheckBox() : ICheckBox
    fun RadioButton(label: String = "", selected: Boolean = false) : IRadioButton
    fun GradientSlider(
            minValue : Float = 0f,
            maxValue : Float = 1f,
            label: String = "") : IGradientSlider
    fun Label( text: String = "") : ILabel
    fun EditableLabel( text: String = "") : IEditableLabel
    fun ScrollBar(
            orientation: sgui.Orientation,
            context: IComponent,
            minScroll: Int = 0,
            maxScroll: Int = 100,
            startScroll: Int = 0,
            scrollWidth : Int = 10) : IScrollBar
    fun ScrollContainer( component: IComponent) : IScrollContainer
    fun ToggleButton(startChecked: Boolean = false) : IToggleButton
    fun CrossPanel(constructor: (CrossInitializer.()->Unit)? = null ) : ICrossPanel
    fun TabbedPane( ): ITabbedPane
    fun <T> ComboBox( things: Array<T>) : IComboBox<T>
    fun <T> TreeView() : ITreeView<T>
    fun TextField() : ITextField
    fun IntField(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE, allowsNegative: Boolean = false) : IIntField
    fun FloatField(min: Float = Float.NEGATIVE_INFINITY, max: Float = Float.POSITIVE_INFINITY, allowsNegative: Boolean = true) : IFloatField

    fun TextArea() : ITextArea

    fun Separator( orientation: sgui.Orientation) : ISeparator
    fun ColorSquare( color: SColor = Colors.BLACK) : IColorSquare

    fun <T:Any> BoxList(boxWidth: Int, boxHeight: Int, entries: Collection<T>? = null) : IBoxList<T>

    fun Slider(min: Int = 0, max:Int = 100, value: Int = 0) : ISlider

    fun ImageBox( img: IImage? = null) : IImageBox
}