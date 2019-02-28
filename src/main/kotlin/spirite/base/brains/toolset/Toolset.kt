package spirite.base.brains.toolset

import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.NillImageDrawer
import spirite.base.imageData.mediums.magLev.MaglevImageDrawer
import spirite.base.imageData.mediums.magLev.MaglevMedium

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
    val Reshape = Reshaper( this)
    val ColorChanger = ColorChanger( this)
    val ColorPicker = ColorPicker( this)
    val Stencil = StencilTool(this)


    private val defaultTools = listOf(
            Pen, Eraser, Fill, ShapeSelection, FreeSelection, Rigger, Move, Pixel, Crop, Flip, Reshape,
            ColorChanger, ColorPicker, Stencil)
    private val maglevTools = listOf(
            Pen, Eraser, Pixel, Flip, Reshape, ColorChanger)
    fun toolsForDrawer(drawer: IImageDrawer) : List<Tool> {
        return when( drawer) {
            is NillImageDrawer -> listOf(Pen)
            is MaglevImageDrawer -> maglevTools
            else -> defaultTools
        }
    }
}