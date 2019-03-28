package spirite.base.pen.behaviors

import rb.vectrix.compaction.FloatCompactor
import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.deformation.StrokeDeformation
import spirite.base.imageData.deformation.StrokeDeformationPiece
import spirite.base.imageData.drawer.IImageDrawer.IDeformDrawer
import spirite.base.pen.Penner
import spirite.base.util.ColorARGB32Normal
import spirite.base.util.Colors
import spirite.gui.components.basic.events.MouseEvent.MouseButton
import spirite.gui.components.basic.events.MouseEvent.MouseButton.*
import spirite.gui.views.work.WorkSectionView

private val DefaultStrokeColors = listOf(
        0xff0000,
        0x00ff00,
        0x0000ff,
        0xffff00,
        0xff00ff,
        0x000000,
        0xffffff)

class StrokeDeformComposingBehavior(
        penner: Penner,
        val drawer : IDeformDrawer)
    : DrawnPennerBehavior(penner)
{
    private var xCompact : FloatCompactor? = null
    private var yCompact : FloatCompactor? = null

    private var from = true
    private var fromStrokes = mutableListOf<Pair<FloatArray, FloatArray>>()
    private var toStrokes = mutableListOf<Pair<FloatArray,FloatArray>>()

    override fun onStart() {
        xCompact = FloatCompactor()
        yCompact = FloatCompactor()
    }

    override fun onEnd() {
    }

    override fun onTock() {
    }

    override fun onMove() {
        xCompact?.add(penner.xf)
        yCompact?.add(penner.yf)
    }

    override fun onPenDown(button: MouseButton) {
        when(button) {
            LEFT -> {
                xCompact = FloatCompactor()
                yCompact = FloatCompactor()
            }
            RIGHT -> {
                from = !from
            }
            CENTER -> {
                val fromCt = fromStrokes.size
                val toCt = toStrokes.size

                drawer.deform(
                        StrokeDeformation(
                                fromStrokes.zip(toStrokes) { from, to ->
                                    StrokeDeformationPiece(from.first, from.second, to.first, to.second)
                                }))
                super.onEnd()
            }
        }
    }

    override fun onPenUp() {
        val xCompact = xCompact
        val yCompact = yCompact
        if( xCompact != null && yCompact != null)
        {
            val listToAddTo = if(from) fromStrokes else toStrokes
            listToAddTo.add(Pair(xCompact.toArray(), yCompact.toArray()))
            this.xCompact = null
            this.yCompact = null
        }
    }



    override fun paintOverlay(gc: GraphicsContext, view: WorkSectionView) {
        fromStrokes.forEachIndexed { index, (x_, y_) ->
            gc.color = ColorARGB32Normal( DefaultStrokeColors[index])
            gc.drawPolyLine(x_, y_, x_.size)
        }
        toStrokes.forEachIndexed { index, (x_, y_) ->
            gc.color = ColorARGB32Normal( DefaultStrokeColors[index])
            gc.drawPolyLine(x_, y_, x_.size)
        }
        val xCompact = xCompact
        val yCompact = yCompact
        if( xCompact != null && yCompact != null) {
            gc.color = Colors.MAGENTA
            val xs = xCompact.toArray()
            val ys = yCompact.toArray()
            gc.drawPolyLine(xs, ys, xs.size)
        }
    }
}