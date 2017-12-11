package spirite.base.pen

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.GraphicsContext.Composite
import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.mediums.BuiltMediumData
import spirite.base.image_data.mediums.DoerOnRaw
import spirite.base.image_data.selection.SelectionMask
import spirite.base.pen.PenTraits.PenState
import spirite.base.util.MUtil
import spirite.base.util.compaction.FloatCompactor
import spirite.base.util.interpolation.CubicSplineInterpolator2D
import spirite.base.util.interpolation.Interpolator2D
import spirite.base.util.interpolation.Interpolator2D.InterpolatedPoint
import spirite.base.util.linear.MatTrans
import spirite.base.util.linear.Vec2
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType
import java.util.*
import java.util.concurrent.atomic.AtomicReference

abstract class StrokeEngine {

    // Pen States
    protected var oldX: Float = 0f
    protected var oldY: Float = 0f
    protected var oldP: Float = 0f
    protected var newX: Float = 0f
    protected var newY: Float = 0f
    protected var newP: Float = 0f
    protected var rawX: Float = 0f
    protected var rawY: Float = 0f
    protected var rawP: Float = 0f    // Needed to prevent UndoAction from double-tranforming

    var state = STATE.READY; protected set
    protected var prec = ArrayList<PenState>()    // Recording of raw states

    // Context
    var params: StrokeParams? = null; protected set
    var imageData: BuildingMediumData? = null; protected set
    var lastSelection: SelectionMask? = null; protected set

    // Interpolation
    private var _interpolator: Interpolator2D? = null

    /** Methods used to record the Stroke so that it can be repeated
     * Could possibly combine them into a single class  */
    val history: Array<PenState> get() = prec.toTypedArray()

    // =============
    // ==== Abstract Methods
    protected abstract fun onStart(trans: MatTrans, w: Int, h: Int)
    protected abstract fun drawToLayer(points: DrawPoints, permanent: Boolean): Boolean
    protected abstract fun prepareDisplayLayer()
    protected abstract fun onEnd()
    protected abstract fun drawDisplayLayer(gc: GraphicsContext)


    enum class STATE {READY, DRAWING}
    enum class Method constructor(val fileId: Int) {
        BASIC(0),
        ERASE(1),
        PIXEL(2);


        companion object {
            fun fromFileId(fid: Int): Method? {
                for (m in Method.values())
                    if (m.fileId == fid)
                        return m
                return null
            }
        }
    }

    /**
     * Starts a new stroke using the workspace's current selection as the
     * selection mask
     *
     * @return true if the data has been changed, false otherwise.
     */
    fun startStroke(
            params: StrokeParams,
            ps: PenState,
            building: BuildingMediumData,
            selection: SelectionMask?): Boolean
    {
        this.imageData = building

        val changed = AtomicReference(false)
        building.doOnBuiltData { built ->

            if (!prepareStroke(params, built, selection))
                return@doOnBuiltData
            buildInterpolator(params, ps)

            if (lastSelection != null) {
                /*			selectionMask = new BufferedImage(
						data.getWidth(), data.getHeight(), Globals.BI_FORMAT);
				MUtil.clearImage(selectionMask);

				Graphics2D g2 = (Graphics2D)selectionMask.getGraphics();
				g2.translate(sel.offsetX, sel.offsetY);
				sel.selection.drawSelectionMask(g2);
				g2.dispose();*/
            }

            // Starts recording the Pen States
            prec = ArrayList()
            val layerSpace = built.screenToSource.transform(Vec2(ps.x, ps.y))

            oldX = layerSpace.x
            oldY = layerSpace.y
            oldP = ps.pressure
            newX = layerSpace.x
            newY = layerSpace.y
            newP = ps.pressure
            rawX = ps.x
            rawY = ps.y
            rawP = ps.pressure
            prec.add(ps)

            state = STATE.DRAWING



            drawToLayer(DrawPoints(
                    floatArrayOf(ps.x),
                    floatArrayOf(ps.y),
                    floatArrayOf(params.dynamics.getSize(ps))),
                    false)

            changed.set(true)
        }

        //		return startDrawStroke( newState);
        return changed.get()
    }

    private fun prepareStroke(
            params: StrokeParams?,
            built: BuiltMediumData?,
            selection: SelectionMask?): Boolean {
        if (built == null)
            return false

        this.params = params


        lastSelection = selection
        onStart(built.screenToSource, built.compositeWidth, built.compositeHeight)
        prepareDisplayLayer()
        return true
    }

    private fun buildInterpolator(params: StrokeParams, ps: PenState) {
        when (params.interpolationMethod) {
            StrokeParams.InterpolationMethod.CUBIC_SPLINE -> _interpolator = CubicSplineInterpolator2D(null, true)
            else -> _interpolator = null
        }
        if (_interpolator != null) _interpolator!!.addPoint(ps.x, ps.y)
    }

    fun stepStroke(ps: PenState): Boolean {
        val changed = AtomicReference(false)
        imageData!!.doOnBuiltData { built ->

            val layerSpace = built!!.screenToSource.transform(Vec2(ps.x, ps.y))
            newX = layerSpace.x
            newY = layerSpace.y
            newP = ps.pressure
            rawX = ps.x
            rawY = ps.y
            rawP = ps.pressure

            if (state != STATE.DRAWING ) {
                MDebug.handleWarning(WarningType.STRUCTURAL, this, "Data Dropped mid-stroke (possible loss of Undo functionality)")
                return@doOnBuiltData
            }

            if (oldX != newX || oldY != newY) {
                prec.add(PenState(rawX, rawY, rawP))
                if (_interpolator != null)
                    _interpolator!!.addPoint(rawX, rawY)

                prepareDisplayLayer()
                changed.set(this.drawToLayer(buildPoints(_interpolator, prec, params), false))
            }

            oldX = newX
            oldY = newY
            oldP = newP
        }

        return changed.get()
    }


    /** Finalizes the stroke, resetting the state, anchoring the strokeLayer
     * to the data, and flushing the used resources.  */
    fun endStroke() {

        state = STATE.READY

        if (imageData != null) {
            imageData!!.doOnBuiltData { built -> built.doOnRaw(DoerOnRaw { raw -> drawStrokeLayer(raw.graphics) }) }
        }

        onEnd()
        _interpolator = null
    }


    /**
     * In order to speed up undo/redo, certain Stroke Engines will batch all
     * draw commands into a single command instead of updating the stroke layer
     * repeatedly.
     */
    fun batchDraw(params: StrokeParams, points: Array<PenState>, builtImage: BuiltMediumData?, mask: SelectionMask?) {
        prepareStroke(params, builtImage, mask)
        buildInterpolator(params, points[0])

        if (_interpolator != null) {
            // NOTE: startStroke already adds the first Point into the
            //	Interpolator, so we start at point 2 (index 1).
            for (i in 1 until points.size) {
                _interpolator!!.addPoint(points[i].x, points[i].y)
            }
        }

        this.drawToLayer(buildPoints(_interpolator, Arrays.asList(*points), this.params), false)

        builtImage?.doOnRaw( DoerOnRaw{ raw -> drawStrokeLayer(raw.graphics) })

        _interpolator = null
    }

    fun batchDraw(points: DrawPoints, builtImage: BuiltMediumData?, mask: SelectionMask?) {
        prepareStroke(null, builtImage, mask)

        this.drawToLayer(points, false)

        builtImage?.doOnRaw( DoerOnRaw { raw -> drawStrokeLayer(raw.graphics) })
    }


    // Draws the Stroke Layer onto the graphics
    fun drawStrokeLayer(gc: GraphicsContext) {
        val oldAlpha = gc.alpha
        val oldComp = gc.composite

        when (params!!.method) {
            StrokeEngine.Method.BASIC, StrokeEngine.Method.PIXEL -> gc.setComposite(Composite.SRC_OVER, params!!.alpha)
            StrokeEngine.Method.ERASE -> gc.setComposite(Composite.DST_OUT, params!!.alpha)
        }
        drawDisplayLayer(gc)
        gc.setComposite(oldComp, oldAlpha)
    }


    companion object {

        // The Interpolator tick distance.  Lower means smoother but more rendering time (especially with Maglev layers)
        val DIFF = 1.0

        fun buildPoints(localInterpolator: Interpolator2D?, penStates: List<PenState>, params: StrokeParams?): DrawPoints {
            var buff: PenState


            if (localInterpolator != null) {
                if (penStates.size == 0)
                    return DrawPoints(FloatArray(0), FloatArray(0), FloatArray(0))
                if (penStates.size == 1)
                    return DrawPoints(floatArrayOf(penStates[0].x), floatArrayOf(penStates[0].y), floatArrayOf(params!!.dynamics.getSize(penStates[0])))
                val fcx = FloatCompactor()
                val fcy = FloatCompactor()
                val fcw = FloatCompactor()

                var localInterpos = 0f
                var ip: InterpolatedPoint = localInterpolator.evalExt(localInterpos)
                buff = PenState(ip.x, ip.y, MUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp))
                fcx.add(ip.x)
                fcy.add(ip.y)
                fcw.add(params!!.dynamics.getSize(buff))

                while (localInterpos + DIFF < localInterpolator.curveLength) {
                    localInterpos += DIFF.toFloat()
                    ip = localInterpolator.evalExt(localInterpos)

                    buff = PenState(ip.x, ip.y, MUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp))
                    fcx.add(ip.x)
                    fcy.add(ip.y)
                    fcw.add(params.dynamics.getSize(buff))
                }

                return DrawPoints(fcx.toArray(), fcy.toArray(), fcw.toArray())
            } else {
                val xs = FloatArray(penStates.size)
                val ys = FloatArray(penStates.size)
                val ws = FloatArray(penStates.size)
                for (i in penStates.indices) {
                    xs[i] = penStates[i].x
                    ys[i] = penStates[i].y
                    ws[i] = params!!.dynamics.getSize(penStates[i])
                }
                return DrawPoints(xs, ys, ws)
            }
        }

        fun buildIndexedPoints(localInterpolator: Interpolator2D?, penStates: List<PenState>, params: StrokeParams): IndexedDrawPoints {
            var buff: PenState


            if (localInterpolator != null) {
                if (penStates.size == 0)
                    return IndexedDrawPoints(FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0))
                if (penStates.size == 1)
                    return IndexedDrawPoints(floatArrayOf(penStates[0].x), floatArrayOf(penStates[0].y), floatArrayOf(params.dynamics.getSize(penStates[0])), floatArrayOf(0.0f))
                val fcx = FloatCompactor()
                val fcy = FloatCompactor()
                val fcw = FloatCompactor()
                val fct = FloatCompactor()

                var localInterpos = 0f
                var ip: InterpolatedPoint = localInterpolator.evalExt(localInterpos)
                buff = PenState(ip.x, ip.y, MUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp))
                fcx.add(ip.x)
                fcy.add(ip.y)
                fcw.add(params.dynamics.getSize(buff))
                fct.add(ip.left + ip.lerp)

                while (localInterpos + DIFF < localInterpolator.curveLength) {
                    localInterpos += DIFF.toFloat()
                    ip = localInterpolator.evalExt(localInterpos)

                    buff = PenState(ip.x, ip.y, MUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp))
                    fcx.add(ip.x)
                    fcy.add(ip.y)
                    fcw.add(params.dynamics.getSize(buff))
                    fct.add(ip.left + ip.lerp)
                }

                return IndexedDrawPoints(fcx.toArray(), fcy.toArray(), fcw.toArray(), fct.toArray())
            } else {
                val xs = FloatArray(penStates.size)
                val ys = FloatArray(penStates.size)
                val ws = FloatArray(penStates.size)
                val ts = FloatArray(penStates.size)
                for (i in penStates.indices) {
                    xs[i] = penStates[i].x
                    ys[i] = penStates[i].y
                    ws[i] = params.dynamics.getSize(penStates[i])
                    ts[i] = i.toFloat()
                }
                return IndexedDrawPoints(xs, ys, ws, ts)
            }
        }
    }
}
