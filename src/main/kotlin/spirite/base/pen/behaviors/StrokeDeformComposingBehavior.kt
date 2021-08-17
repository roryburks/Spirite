package spirite.base.pen.behaviors

import rb.glow.ColorARGB32Normal
import rb.glow.Colors
import rb.glow.IGraphicsContext
import rb.vectrix.compaction.FloatCompactor
import rb.vectrix.interpolation.CubicSplineInterpolator2D
import rb.vectrix.mathUtil.d
import sgui.core.components.events.MouseEvent.MouseButton
import sgui.core.components.events.MouseEvent.MouseButton.*
import spirite.base.imageData.deformation.StrokeDeformationPiece
import spirite.base.imageData.deformation.StrokeDeformationV2
import spirite.base.graphics.drawer.IImageDrawer.IDeformDrawer
import spirite.base.pen.Penner
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
                        StrokeDeformationV2(
                                fromStrokes.zip(toStrokes) { from, to ->
                                    StrokeDeformationPiece(from.first, from.second, to.first, to.second)
                                }))
                end()
            }
        }
    }

    override fun onPenUp() {
        val xCompact = xCompact
        val yCompact = yCompact
        if( xCompact != null && yCompact != null)
        {

            val interpolator = CubicSplineInterpolator2D(xCompact.toArray(), yCompact.toArray(), true)

            val builtFcX = FloatCompactor()
            val builtFcY = FloatCompactor()

            var interpos = 0f


            fun addPoint() {
                val ip = interpolator.evalExt(interpos)
                builtFcX.add(ip.x)
                builtFcY.add(ip.y)
            }
            addPoint()

            while( interpos + 1.0f < interpolator.curveLength) {
                interpos += 1.0f
                addPoint()
            }

            val listToAddTo = if(from) fromStrokes else toStrokes
            listToAddTo.add(Pair(builtFcX.toArray(), builtFcY.toArray()))
            this.xCompact = null
            this.yCompact = null
        }
    }



    override fun paintOverlay(gc: IGraphicsContext, view: WorkSectionView) {
        fromStrokes.forEachIndexed { index, (x_, y_) ->
            gc.color = ColorARGB32Normal(DefaultStrokeColors[index])
            gc.drawPolyLine(x_.map { it.d }, y_.map { it.d }, x_.size)
        }
        toStrokes.forEachIndexed { index, (x_, y_) ->
            gc.color = ColorARGB32Normal(DefaultStrokeColors[index])
            gc.drawPolyLine(x_.map { it.d }, y_.map { it.d }, x_.size)
        }
        val xCompact = xCompact
        val yCompact = yCompact
        if( xCompact != null && yCompact != null) {
            gc.color = Colors.MAGENTA
            val xs = xCompact.toArray().map { it.d }
            val ys = yCompact.toArray().map { it.d }

            gc.drawPolyLine(xs, ys, xs.size)
        }
    }
}