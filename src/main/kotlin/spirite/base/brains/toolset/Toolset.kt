package spirite.base.brains.toolset

import spirite.base.imageData.mediums.drawer.IImageDrawer
import spirite.base.imageData.mediums.drawer.NillImageDrawer

/** A Toolset is a complete set of every tool */
class Toolset( internal val manager: ToolsetManager) {
    val Pen = Pen(this)
    val Eraser = Eraser(this)
    val Fill = Fill( this)
    val ShapeSelection = ShapeSelection( this)
    val FreeSelection = FreeSelection( this)
    val Move = Move( this)
    val Pixel = Pixel( this)
    val Crop = Crop( this)
    val Rigger = Rigger( this)
    val Flip = Flip( this)
    val Resize = Resize( this)
    val ColorChanger = ColorChanger( this)
    val ColorPicker = ColorPicker( this)


    private val defaultTools = listOf(
            Pen, Eraser, Fill, ShapeSelection, FreeSelection, Move, Pixel, Crop, Flip, Resize, ColorChanger, ColorPicker)
    fun toolsForDrawer(drawer: IImageDrawer) : List<Tool> {
        return when( drawer) {
            is NillImageDrawer -> listOf(Pen)
            else -> defaultTools
        }
    }
}