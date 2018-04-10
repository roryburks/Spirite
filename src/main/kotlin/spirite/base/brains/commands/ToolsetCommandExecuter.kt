package spirite.base.brains.commands

import spirite.base.brains.toolset.IToolsetManager
import spirite.base.brains.commands.ToolsetCommandExecuter.ToolCommand.*

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
            else -> return false
        }
        return true
    }
}