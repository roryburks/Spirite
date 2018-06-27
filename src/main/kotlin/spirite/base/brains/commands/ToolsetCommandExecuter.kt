package spirite.base.brains.commands

import spirite.base.brains.toolset.IToolsetManager
import spirite.base.brains.commands.ToolsetCommandExecuter.ToolCommand.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

class ToolsetCommandExecuter(val toolsetManager: IToolsetManager) : ICommandExecuter
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

        DecreasePenSize("decreaseSize"),
        IncreasePenSize("increaseSize"),
        ;

        override val commandString: String get() = "tool.$string"
    }
    override val validCommands: List<String> get() = ToolCommand.values().map { it.string }
    override val domain: String get() = "tool"

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
            else -> return false
        }
        return true
    }
}