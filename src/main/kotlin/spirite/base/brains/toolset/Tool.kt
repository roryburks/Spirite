package spirite.base.brains.toolset

import spirite.base.brains.Bindable
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand
import spirite.base.brains.commands.ICommand
import spirite.base.util.linear.Vec2
import kotlin.reflect.KProperty

abstract class Tool(
        private val toolset: Toolset
) {
    abstract val description: String
    abstract val iconX : Int
    abstract val iconY : Int

    protected val scheme = ToolScheme()
    val properties: List<ToolProperty<*>> get() = scheme.properties

    inner class ToolScheme {
        internal val properties = mutableListOf<ToolProperty<*>>()

        fun <T,R> Property( t: T) : ToolPropDelegate<R> where T : ToolProperty<R> {
            properties.add(t)
            t.valueBind.addListener { new, old -> toolset.manager.triggerToolsetChanged(this@Tool, t) }
            return ToolPropDelegate(t)
        }

        inner class ToolPropDelegate<T> internal constructor(val toolProperty: ToolProperty<T>) {
            operator fun getValue(thisRef: Any, prop: KProperty<*>) = toolProperty.value
            operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {toolProperty.value = value}
        }
    }
}

sealed class ToolProperty<T>( default: T) {
    abstract val hrName: String

    val valueBind = Bindable(default)
    var value by valueBind
}

class SliderProperty(override val hrName: String, default: Float, val min: Float, val max: Float) : ToolProperty<Float>(default)
class SizeProperty( override val hrName: String, default: Float) : ToolProperty<Float>(default)
class CheckBoxProperty( override val hrName: String, default: Boolean) : ToolProperty<Boolean>(default)
class DropDownProperty<T>( override val hrName: String, default: T, val values: Array<T>) : ToolProperty<T>(default)
class RadioButtonProperty<T>( override val hrName: String, default: T, val values: Array<T>) : ToolProperty<T>(default)
class ButtonProperty(override val hrName: String, val command: ICommand) : ToolProperty<Any?>(null)
class FloatBoxProperty(override val hrName: String, default: Float) : ToolProperty<Float>(default)
class DualFloatBoxProperty(override val hrName: String, val label1: String, val label2: String, default: Vec2) : ToolProperty<Vec2>(default)

enum class PenDrawMode( val hrName: String) {
    NORMAL("Normal"),
    KEEP_ALPHA("Preserve Alpha"),
    BEHIND("Behind"),
    ;
    override fun toString() = hrName
}

class Pen( toolset: Toolset) : Tool(toolset){
    override val iconX = 0
    override val iconY = 0
    override val description = "Pen"

    var alpha by scheme.Property(SliderProperty( "Opacity", 1.0f, 0f, 1f ))
    var width by scheme.Property(SizeProperty("Width", 5.0f))
    var hard by scheme.Property(CheckBoxProperty("Hard Edged",false))
    var mode by scheme.Property(DropDownProperty("Draw Mode", PenDrawMode.NORMAL, PenDrawMode.values()))
}
class Eraser( toolset: Toolset) : Tool(toolset){
    override val iconX = 1
    override val iconY = 0
    override val description = "Eraser"

    var alpha by scheme.Property(SliderProperty( "Opacity", 1.0f, 0f, 1f ))
    var width by scheme.Property(SizeProperty("Width", 5.0f))
    var hard by scheme.Property(CheckBoxProperty("Hard Edged",false))
}
class Fill( toolset: Toolset) : Tool(toolset){
    override val iconX = 2
    override val iconY = 0
    override val description = "Fill"
}

enum class BoxSelectionShape( val hrName : String) {
    RECTANGLE("Rectangle"),
    OVAL("Oval"),
    ;
    override fun toString() = hrName
}
class ShapeSelection( toolset: Toolset) : Tool(toolset){
    override val iconX = 3
    override val iconY = 0
    override val description = "Shape Selection"

    var shape by scheme.Property(DropDownProperty("Shape", BoxSelectionShape.RECTANGLE, BoxSelectionShape.values()))
}
class FreeSelection( toolset: Toolset) : Tool(toolset){
    override val iconX = 0
    override val iconY = 1
    override val description = "Free Selection"
}
class Move( toolset: Toolset) : Tool(toolset){
    override val iconX = 1
    override val iconY = 1
    override val description = "Move"
}
class Pixel( toolset: Toolset) : Tool(toolset){
    override val iconX = 2
    override val iconY = 1
    override val description = "Pixel"

    var alpha by scheme.Property(SliderProperty( "Opacity", 1.0f, 0f, 1f ))
    var mode by scheme.Property(DropDownProperty("Draw Mode", PenDrawMode.NORMAL, PenDrawMode.values()))
}
class Crop( toolset: Toolset) : Tool(toolset){
    override val iconX = 3
    override val iconY = 1
    override val description = "Cropper"

    var button by scheme.Property(ButtonProperty("Crop Selection", DrawCommand.CROP_SELECTION))
    var quickCrop by scheme.Property(CheckBoxProperty("Crop on Finish", false))
    var shrinkOnly by scheme.Property(CheckBoxProperty("Shrink-only Crop", false))
}
class Rigger( toolset: Toolset) : Tool(toolset){
    override val iconX = 0
    override val iconY = 2
    override val description = "Rig"
}

enum class FlipMode( val hrName : String) {
    HORIZONTAL("Horizontal Flipping"),
    VERTICAL("Vertical Flipping"),
    BY_MOVEMENT("Determine from Movement"),
    ;
    override fun toString() = hrName
}
class Flip( toolset: Toolset) : Tool(toolset){
    override val iconX = 1
    override val iconY = 2
    override val description = "Flipper"

    var flipMode by scheme.Property(RadioButtonProperty("Flip Mode", FlipMode.BY_MOVEMENT, FlipMode.values()))
}
class Reshaper(toolset: Toolset) : Tool(toolset){
    override val iconX = 2
    override val iconY = 2
    override val description = "Reshaper"

    var applyTransform by scheme.Property(ButtonProperty("Apply Transform", DrawCommand.APPLY_TRANFORM))
    var scale : Vec2 by scheme.Property(DualFloatBoxProperty("Scale", "x","y", Vec2(1f,1f)))
    var translation : Vec2 by scheme.Property(DualFloatBoxProperty("Translation", "x","y", Vec2(1f,1f)))
    var rotation by scheme.Property(FloatBoxProperty("Rotation", 0f))

}

enum class ColorChangeScopes( val hrName: String) {
    LOCAL("Local"),
    GROUP("Entire Layer/Group"),
    PROJECT("Entire Project")
    ;
    override fun toString() = hrName
}
enum class ColorChangeMode( val hrName: String) {
    CHECK_ALL("Check Alpha"),
    IGNORE_ALPHA("Ignore Alpha"),
    AUTO("Change All")
    ;
    override fun toString() = hrName
}
class ColorChanger( toolset: Toolset) : Tool(toolset){
    override val iconX = 3
    override val iconY = 2
    override val description = "Color Changer"

    var scope by scheme.Property(DropDownProperty("Scope", ColorChangeScopes.LOCAL, ColorChangeScopes.values()))
    var mode by scheme.Property(RadioButtonProperty("Apply Mode", ColorChangeMode.CHECK_ALL, ColorChangeMode.values()))
}
class ColorPicker( toolset: Toolset) : Tool(toolset){
    override val iconX = 0
    override val iconY = 3
    override val description = "Color Picker"
}