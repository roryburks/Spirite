package spirite.base.brains.commands

import spirite.base.brains.KeyCommand
import spirite.base.brains.commands.ToolsetCommandExecutor.ToolCommand.*
import spirite.base.brains.toolset.DropDownProperty
import spirite.base.brains.toolset.IToolsetManager
import spirite.hybrid.MDebug
import kotlin.math.ceil
import kotlin.math.floor

class ToolsetCommandExecutor(val toolsetManager: IToolsetManager) : ICommandExecutor
{
    enum class ToolCommand(val string: String) : ICommand {
        Pen( "Pen"),
        ColorChanger("ColorChanger"),
        ColorPicker("ColorPicker"),
        Crop("Crop"),
        Eraser("Eraser"),
        Fill( "Fill"),
        Flip("Flip"),
        FreeSelection("FreeSelection"),
        Move("Move"),
        Pixel("Pixel"),
        Reshape( "Reshape"),
        Rigger("Rigger"),
        ShapeSelection("ShapeSelection"),
        MagneticFill("MagneticFill"),

        DecreasePenSize("decreaseSize"),
        IncreasePenSize("increaseSize"),

        SetMode_1("setMode:1"),
        SetMode_2("setMode:2"),
        SetMode_3("setMode:3"),
        SetMode_4("setMode:4"),
        ;

        override val commandString: String get() = "tool.$string"
        override val keyCommand: KeyCommand get() = KeyCommand(commandString)
    }
    override val validCommands: List<String> get() = ToolCommand.values().map { it.string }
    override val domain: String get() = "tool"

    fun setMode(i: Int) {
        toolsetManager.selectedTool.properties
                .filterIsInstance<DropDownProperty<*>>()
                .firstOrNull()
                ?.setNthOption(i)
    }

    override fun executeCommand(string: String, extra: Any?): Boolean {
        when(string) {
            Pen.string -> toolsetManager.selectedTool = toolsetManager.toolset.Pen
            ColorChanger.string -> toolsetManager.selectedTool = toolsetManager.toolset.ColorChanger
            ColorPicker.string -> toolsetManager.selectedTool = toolsetManager.toolset.ColorPicker
            Crop.string -> toolsetManager.selectedTool = toolsetManager.toolset.Crop
            Eraser.string -> toolsetManager.selectedTool = toolsetManager.toolset.Eraser
            Fill.string -> toolsetManager.selectedTool = toolsetManager.toolset.Fill
            Flip.string -> toolsetManager.selectedTool = toolsetManager.toolset.Flip
            FreeSelection.string -> toolsetManager.selectedTool = toolsetManager.toolset.FreeSelection
            Move.string -> toolsetManager.selectedTool = toolsetManager.toolset.Move
            Pixel.string -> toolsetManager.selectedTool = toolsetManager.toolset.Pixel
            Reshape.string -> toolsetManager.selectedTool = toolsetManager.toolset.Reshape
            Rigger.string -> toolsetManager.selectedTool = toolsetManager.toolset.Rigger
            ShapeSelection.string -> toolsetManager.selectedTool = toolsetManager.toolset.ShapeSelection
            MagneticFill.string -> toolsetManager.selectedTool = toolsetManager.toolset.MagneticFill

            SetMode_1.string -> setMode(0)
            SetMode_2.string -> setMode(1)
            SetMode_3.string -> setMode(2)
            SetMode_4.string -> setMode(3)

            DecreasePenSize.string -> {
                val selected = toolsetManager.selectedTool

                fun decrease( f: Float) : Float
                {
                    return when{
                        f < 1 -> 1f
                        f <= 20 -> ceil(f-1f)
                        else -> ceil(f-2f)
                    }
                }

                when( selected)
                {
                    is spirite.base.brains.toolset.Pen -> selected.width = decrease(selected.width)
                    is spirite.base.brains.toolset.Eraser -> selected.width = decrease(selected.width)
                }
            }
            IncreasePenSize.string -> {
                val selected = toolsetManager.selectedTool

                fun increase(f : Float) : Float
                {
                    return when{
                        f < 20 -> floor(f+1f)
                        else -> floor(f + 2f)
                    }
                }

                when( selected)
                {
                    is spirite.base.brains.toolset.Pen -> selected.width = increase(selected.width)
                    is spirite.base.brains.toolset.Eraser -> selected.width = increase(selected.width)
                }
            }

            else -> MDebug.handleWarning(MDebug.WarningType.REFERENCE, "Unrecognized command: tool.$string")
        }
        return true
    }
}