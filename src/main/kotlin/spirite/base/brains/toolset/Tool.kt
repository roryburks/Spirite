package spirite.base.brains.toolset

import spirite.base.brains.Bindable
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

abstract class ToolProperty<T>( default: T) {
    abstract val hrName: String

    val valueBind = Bindable(default)
    var value by valueBind
}

class SliderProperty(default: Float, override val hrName: String) : ToolProperty<Float>(default) {}
class SizeProperty( default: Float, override val hrName: String) : ToolProperty<Float>(default) {}
class CheckBoxProperty( default: Boolean, override val hrName: String) : ToolProperty<Boolean>(default) {}
class DropDownProperty<T>( default: T, override val hrName: String) : ToolProperty<T>(default) where T : Enum<T>{}

enum class PenDrawMode( val hrName: String) {
    NORMAL("Normal"),
    KEEP_ALPHA("Preserve Alpha"),
    BEHIND("Behind");

    override fun toString() = hrName
}

class Pen( toolset: Toolset) : Tool(toolset){
    override val iconX = 0
    override val iconY = 0
    override val description = "Pen"

    var alpha by scheme.Property(SliderProperty( 1.0f, "Opacity" ))
    var width by scheme.Property(SizeProperty(5.0f, "Width"))
    var hard by scheme.Property(CheckBoxProperty(false, "Hard Edged"))
    var mode by scheme.Property(DropDownProperty(PenDrawMode.NORMAL, "Draw Mode"))
}
class Eraser( toolset: Toolset) : Tool(toolset){
    override val iconX = 1
    override val iconY = 0
    override val description = "Eraser"
}
class Fill( toolset: Toolset) : Tool(toolset){
    override val iconX = 2
    override val iconY = 0
    override val description = "Fill"
}
class ShapeSelection( toolset: Toolset) : Tool(toolset){
    override val iconX = 3
    override val iconY = 0
    override val description = "Shape Selection"
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
}
class Crop( toolset: Toolset) : Tool(toolset){
    override val iconX = 3
    override val iconY = 1
    override val description = "Cropper"
}
class Rigger( toolset: Toolset) : Tool(toolset){
    override val iconX = 0
    override val iconY = 2
    override val description = "Rig"
}
class Flip( toolset: Toolset) : Tool(toolset){
    override val iconX = 1
    override val iconY = 2
    override val description = "Flipper"
}
class Resize( toolset: Toolset) : Tool(toolset){
    override val iconX = 2
    override val iconY = 2
    override val description = "Resizer"
}
class ColorChanger( toolset: Toolset) : Tool(toolset){
    override val iconX = 3
    override val iconY = 2
    override val description = "Color Changer"
}
class ColorPicker( toolset: Toolset) : Tool(toolset){
    override val iconX = 0
    override val iconY = 3
    override val description = "Color Picker"
}