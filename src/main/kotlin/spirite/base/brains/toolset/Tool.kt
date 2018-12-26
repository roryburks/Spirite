package spirite.base.brains.toolset

import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.vectrix.linear.MutableTransformF
import rb.vectrix.linear.Vec2f
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand
import spirite.base.brains.commands.ICommand
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
            t.valueBind.addObserver { _, _ -> toolset.manager.triggerToolsetChanged(this@Tool, t) }
            return ToolPropDelegate(t)
        }

        inner class ToolPropDelegate<T> internal constructor(val toolProperty: ToolProperty<T>) {
            operator fun getValue(thisRef: Any, prop: KProperty<*>) = toolProperty.valueBind
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
class ButtonProperty(override val hrName: String, val command: ICommand) : ToolProperty<Boolean>(false)
class FloatBoxProperty(override val hrName: String, default: Float) : ToolProperty<Float>(default)
class DualFloatBoxProperty(override val hrName: String, val label1: String, val label2: String, default: Vec2f) : ToolProperty<Vec2f>(default)

enum class PenDrawMode( val hrName: String) {
    NORMAL("Normal"),
    KEEP_ALPHA("Preserve Alpha"),
    BEHIND("Behind"),
    ;
    override fun toString() = hrName
}

class Pen( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 0
    override val iconY get() = 0
    override val description = "Pen"

    val alphaBind by scheme.Property(SliderProperty( "Opacity", 1.0f, 0f, 1f ))
    var alpha by alphaBind
    val widthBind by scheme.Property(SizeProperty("Width", 5.0f))
    var width by widthBind
    val hardBind by scheme.Property(CheckBoxProperty("Hard Edged",false))
    var hard by hardBind
    val modeBind  by scheme.Property(DropDownProperty("Draw Mode", PenDrawMode.NORMAL, PenDrawMode.values()))
    var mode by modeBind
}
class Eraser( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 1
    override val iconY get() = 0
    override val description = "Eraser"

    val alphaBind by scheme.Property(SliderProperty( "Opacity", 1.0f, 0f, 1f ))
    var alpha by alphaBind
    val widthBind by scheme.Property(SizeProperty("Width", 5.0f))
    var width by widthBind
    val hardBind by scheme.Property(CheckBoxProperty("Hard Edged",false))
    var hard by hardBind
}
class Fill( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 2
    override val iconY get() = 0
    override val description = "Fill"
}

enum class BoxSelectionShape( val hrName : String) {
    RECTANGLE("Rectangle"),
    OVAL("Oval"),
    ;
    override fun toString() = hrName
}
class ShapeSelection( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 3
    override val iconY get() = 0
    override val description = "Shape Selection"

    val shapeBind by scheme.Property(DropDownProperty("Shape", BoxSelectionShape.RECTANGLE, BoxSelectionShape.values()))
    var shape by shapeBind
}
class FreeSelection( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 0
    override val iconY get() = 1
    override val description = "Free Selection"
}
class Move( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 1
    override val iconY get() = 1
    override val description = "Move"
}
class Pixel( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 2
    override val iconY get() = 1
    override val description = "Pixel"

    val alphaBind by scheme.Property(SliderProperty( "Opacity", 1.0f, 0f, 1f ))
    var alpha by alphaBind
    val modeBind by scheme.Property(DropDownProperty("Draw Mode", PenDrawMode.NORMAL, PenDrawMode.values()))
    var mode by modeBind
}
class Crop( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 3
    override val iconY get() = 1
    override val description = "Cropper"

    val buttonBind by scheme.Property(ButtonProperty("Crop Selection", DrawCommand.CROP_SELECTION))
    var button by buttonBind
    val quickCropBind by scheme.Property(CheckBoxProperty("Crop on Finish", false))
    var quickCrop by quickCropBind
    val shrinkOnlyBind by scheme.Property(CheckBoxProperty("Shrink-only Crop", false))
    var shrinkOnly by shrinkOnlyBind
}

enum class WorkspaceScope {
    Node,
    Group,
    Workspace
}
class Rigger( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 0
    override val iconY get() = 2
    override val description = "Rig"

    val scopeBind by scheme.Property(DropDownProperty("Scope", WorkspaceScope.Group, WorkspaceScope.values()))
    var scope by scopeBind
}

enum class FlipMode( val hrName : String) {
    HORIZONTAL("Horizontal Flipping"),
    VERTICAL("Vertical Flipping"),
    BY_MOVEMENT("Determine from Movement"),
    ;
    override fun toString() = hrName
}
class Flip( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 1
    override val iconY get() = 2
    override val description = "Flipper"

    val flipModeBind by scheme.Property(RadioButtonProperty("Flip Mode", FlipMode.BY_MOVEMENT, FlipMode.values()))
    var flipMode by flipModeBind
}
class Reshaper(toolset: Toolset) : Tool(toolset){
    override val iconX get() = 2
    override val iconY get() = 2
    override val description = "Reshaper"

    val transform : MutableTransformF
        get() {
        val t = MutableTransformF.Scale(scale.xf, scale.yf)
        t.preRotate(rotation)
        t.preTranslate(translation.xf, translation.yf)
        return t
    }

    val applyTransformBind by scheme.Property(ButtonProperty("Apply Transform", DrawCommand.APPLY_TRANFORM))
    var applyTransform by applyTransformBind
    val scaleBind by scheme.Property(DualFloatBoxProperty("Scale", "xi","yi", Vec2f(1f,1f)))
    var scale by scaleBind
    val translationBind by scheme.Property(DualFloatBoxProperty("Translation", "xi","yi", Vec2f(0f,0f)))
    var translation by translationBind
    val rotationBind by scheme.Property(FloatBoxProperty("Rotation", 0f))
    var rotation by rotationBind

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
    override val iconX get() = 3
    override val iconY get() = 2
    override val description = "Color Changer"

    val scopeBind by scheme.Property(DropDownProperty("Scope", ColorChangeScopes.LOCAL, ColorChangeScopes.values()))
    var scope by scopeBind
    val modeBind by scheme.Property(RadioButtonProperty("Apply Mode", ColorChangeMode.CHECK_ALL, ColorChangeMode.values()))
    var mode by modeBind
}
class ColorPicker( toolset: Toolset) : Tool(toolset){
    override val iconX get() = 0
    override val iconY get() = 3
    override val description = "Color Picker"
}

class StencilTool(toolset: Toolset) : Tool(toolset) {
    override val iconX: Int get() = 3
    override val iconY: Int get() = 3
    override val description: String get() = "Stencil Tool"

    val clearStencilBind by scheme.Property(ButtonProperty("Clear Stencil", DrawCommand.APPLY_TRANFORM))

}