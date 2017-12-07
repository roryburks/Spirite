package spirite.base.pen

import spirite.base.brains.tools.ToolSchemes
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.GraphicsContext.Composite
import spirite.base.image_data.ImageWorkspace.BuildingMediumData
import spirite.base.image_data.mediums.ABuiltMediumData
import spirite.base.image_data.selection.SelectionMask
import spirite.base.pen.PenTraits.PenDynamics
import spirite.base.pen.PenTraits.PenState
import spirite.base.util.Colors
import spirite.base.util.MUtil
import spirite.base.util.compaction.FloatCompactor
import spirite.base.util.interpolation.CubicSplineInterpolator2D
import spirite.base.util.interpolation.Interpolator2D
import spirite.base.util.interpolation.Interpolator2D.InterpolatedPoint
import spirite.base.util.linear.Vec2
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType

import java.util.ArrayList
import java.util.Arrays
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
    var state = STATE.READY
        protected set
    protected var prec = ArrayList<PenState>()    // Recording of raw states

    // Context
    // :::: Get's
    var params: StrokeEngine.StrokeParams? = null
        protected set
    var imageData: BuildingMediumData? = null
        protected set
    var lastSelection: SelectionMask? = null
        protected set

    // Interpolation
    private var _interpolator: Interpolator2D? = null

    /** Methods used to record the Stroke so that it can be repeated
     * Could possibly combine them into a single class  */
    val history: Array<PenState>
        get() {
            val array = arrayOfNulls<PenState>(prec.size)
            return prec.toTypedArray()
        }

    // =============
    // ==== Abstract Methods
    protected abstract fun drawToLayer(points: DrawPoints, permanent: Boolean, built: ABuiltMediumData?): Boolean

    protected abstract fun prepareDisplayLayer()
    protected abstract fun onStart(built: ABuiltMediumData)
    protected abstract fun onEnd()
    protected abstract fun drawDisplayLayer(gc: GraphicsContext)


    enum class STATE {READY, DRAWING}
    enum class Method private constructor(val fileId: Int) {
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
            val layerSpace = built.convert(Vec2(ps.x, ps.y))

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
                    floatArrayOf(params.getDynamics().getSize(ps))),
                    false, built)

            changed.set(true)
        }

        //		return startDrawStroke( newState);
        return changed.get()
    }

    private fun prepareStroke(
            params: StrokeParams?,
            built: ABuiltMediumData?,
            selection: SelectionMask?): Boolean {
        if (built == null)
            return false

        this.params = params


        lastSelection = selection
        onStart(built)
        prepareDisplayLayer()
        return true
    }

    private fun buildInterpolator(params: StrokeParams, ps: PenState) {
        when (params.getInterpolationMethod()) {
            StrokeEngine.StrokeParams.InterpolationMethod.CUBIC_SPLINE -> _interpolator = CubicSplineInterpolator2D(null, true)
            else -> _interpolator = null
        }
        if (_interpolator != null) _interpolator!!.addPoint(ps.x, ps.y)
    }

    fun stepStroke(ps: PenState): Boolean {
        val changed = AtomicReference(false)
        imageData!!.doOnBuiltData { built ->

            val layerSpace = built!!.convert(Vec2(ps.x, ps.y))
            newX = layerSpace.x
            newY = layerSpace.y
            newP = ps.pressure
            rawX = ps.x
            rawY = ps.y
            rawP = ps.pressure

            if (state != STATE.DRAWING || built == null) {
                MDebug.handleWarning(WarningType.STRUCTURAL, this, "Data Dropped mid-stroke (possible loss of Undo functionality)")
                return@doOnBuiltData
            }

            if (oldX != newX || oldY != newY) {
                prec.add(PenState(rawX, rawY, rawP))
                if (_interpolator != null)
                    _interpolator!!.addPoint(rawX, rawY)

                prepareDisplayLayer()
                changed.set(this.drawToLayer(buildPoints(_interpolator, prec, params), false, built))
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
            imageData!!.doOnBuiltData { built -> built.doOnRaw { raw -> drawStrokeLayer(raw.graphics) } }
        }

        onEnd()
        _interpolator = null
    }


    /**
     * In order to speed up undo/redo, certain Stroke Engines will batch all
     * draw commands into a single command instead of updating the stroke layer
     * repeatedly.
     */
    fun batchDraw(params: StrokeParams, points: Array<PenState>, builtImage: ABuiltMediumData?, mask: SelectionMask?) {
        prepareStroke(params, builtImage, mask)
        buildInterpolator(params, points[0])

        if (_interpolator != null) {
            // NOTE: startStroke already adds the first Point into the
            //	Interpolator, so we start at point 2 (index 1).
            for (i in 1 until points.size) {
                _interpolator!!.addPoint(points[i].x, points[i].y)
            }
        }

        this.drawToLayer(buildPoints(_interpolator, Arrays.asList(*points), this.params), false, builtImage)

        builtImage?.doOnRaw { raw -> drawStrokeLayer(raw.graphics) }

        _interpolator = null
    }

    fun batchDraw(points: DrawPoints, builtImage: ABuiltMediumData?, mask: SelectionMask?) {
        prepareStroke(null, builtImage, mask)

        this.drawToLayer(points, false, builtImage)

        builtImage?.doOnRaw { raw -> drawStrokeLayer(raw.graphics) }
    }


    // Draws the Stroke Layer onto the graphics
    fun drawStrokeLayer(gc: GraphicsContext) {
        val oldAlpha = gc.alpha
        val oldComp = gc.composite

        when (params!!.getMethod()) {
            StrokeEngine.Method.BASIC, StrokeEngine.Method.PIXEL -> gc.setComposite(Composite.SRC_OVER, params!!.getAlpha())
            StrokeEngine.Method.ERASE -> gc.setComposite(Composite.DST_OUT, params!!.getAlpha())
        }
        drawDisplayLayer(gc)
        gc.setComposite(oldComp, oldAlpha)
    }


    /**
     * StrokeParams define the style/tool/options of the Stroke.
     *
     * lock is not actually used yet, but changing data mid-stroke is a
     * bar idea.
     */
    class StrokeParams {


        private var c = Colors.BLACK
        private var method: StrokeEngine.Method = StrokeEngine.Method.BASIC
        private var mode: ToolSchemes.PenDrawMode = ToolSchemes.PenDrawMode.NORMAL
        private var width = 1.0f
        private var alpha = 1.0f
        private var hard = false
        private var dynamics = PenDynamicsConstants.getBasicDynamics()
        var maxWidth = 25
            set(width) {
                if (!isLocked) field = width
            }
        private var interpolationMethod = InterpolationMethod.CUBIC_SPLINE


        /** If Params are locked, they're being used and can't be changed.
         * Only the base StrokeEngine can lock/unlock Params.  Once they are
         * locked they will usually never be unlocked as the UndoEngine needs
         * to remember the saved settings.
         */
        val isLocked = false

        var color: Int
            get() = c
            set(c) {
                if (!isLocked)
                    this.c = c
            }

        var isHard: Boolean
            get() = hard
            set(hard) {
                if (!isLocked)
                    this.hard = hard
            }

        enum class InterpolationMethod {
            NONE,
            CUBIC_SPLINE
        }

        fun getMethod(): StrokeEngine.Method {
            return method
        }

        fun setMethod(method: StrokeEngine.Method) {
            if (!isLocked)
                this.method = method
        }

        fun getMode(): ToolSchemes.PenDrawMode {
            return mode
        }

        fun setMode(mode: ToolSchemes.PenDrawMode) {
            if (!isLocked)
                this.mode = mode
        }

        fun getWidth(): Float {
            return width
        }

        fun setWidth(width: Float) {
            if (!isLocked)
                this.width = width
        }

        fun getAlpha(): Float {
            return alpha
        }

        fun setAlpha(alpha: Float) {
            if (!isLocked)
                this.alpha = Math.max(0.0f, Math.min(1.0f, alpha))
        }

        fun getDynamics(): PenDynamics {
            return dynamics
        }

        fun setDynamics(dynamics: PenDynamics?) {
            if (!isLocked && dynamics != null)
                this.dynamics = dynamics
        }

        fun getInterpolationMethod(): InterpolationMethod {
            return this.interpolationMethod
        }

        fun setInterpolationMethod(method: InterpolationMethod) {
            if (!isLocked) this.interpolationMethod = method
        }

        companion object {


            /**
             * Bakes the PenDynamics of the original StrokeParameters and bakes its dynamics
             * in-place over the given penStates, returning an equivalent StrokeParams, but
             * with Linear Dynamics
             */
            fun bakeAndNormalize(original: StrokeParams, penStates: Array<PenState>): StrokeParams {
                val out = StrokeParams()
                out.alpha = original.alpha
                out.c = original.c
                out.dynamics = PenDynamicsConstants.LinearDynamics()
                out.hard = original.hard
                out.interpolationMethod = original.interpolationMethod
                out.method = original.method
                out.mode = original.mode
                out.width = original.width

                for (i in penStates.indices) {
                    penStates[i] = PenState(penStates[i].x, penStates[i].y,
                            original.getDynamics().getSize(penStates[i]))
                }

                return out
            }
        }
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
                    return DrawPoints(floatArrayOf(penStates[0].x), floatArrayOf(penStates[0].y), floatArrayOf(params!!.getDynamics().getSize(penStates[0])))
                val fcx = FloatCompactor()
                val fcy = FloatCompactor()
                val fcw = FloatCompactor()

                var localInterpos = 0f
                var ip: InterpolatedPoint = localInterpolator.evalExt(localInterpos)
                buff = PenState(ip.x, ip.y, MUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp).toFloat())
                fcx.add(ip.x)
                fcy.add(ip.y)
                fcw.add(params!!.getDynamics().getSize(buff))

                while (localInterpos + DIFF < localInterpolator.curveLength) {
                    localInterpos += DIFF.toFloat()
                    ip = localInterpolator.evalExt(localInterpos)

                    buff = PenState(ip.x, ip.y, MUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp).toFloat())
                    fcx.add(ip.x)
                    fcy.add(ip.y)
                    fcw.add(params.getDynamics().getSize(buff))
                }

                return DrawPoints(fcx.toArray(), fcy.toArray(), fcw.toArray())
            } else {
                val xs = FloatArray(penStates.size)
                val ys = FloatArray(penStates.size)
                val ws = FloatArray(penStates.size)
                for (i in penStates.indices) {
                    xs[i] = penStates[i].x
                    ys[i] = penStates[i].y
                    ws[i] = params!!.getDynamics().getSize(penStates[i])
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
                    return IndexedDrawPoints(floatArrayOf(penStates[0].x), floatArrayOf(penStates[0].y), floatArrayOf(params.getDynamics().getSize(penStates[0])), floatArrayOf(0.0f))
                val fcx = FloatCompactor()
                val fcy = FloatCompactor()
                val fcw = FloatCompactor()
                val fct = FloatCompactor()

                var localInterpos = 0f
                var ip: InterpolatedPoint = localInterpolator.evalExt(localInterpos)
                buff = PenState(ip.x, ip.y, MUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp).toFloat())
                fcx.add(ip.x)
                fcy.add(ip.y)
                fcw.add(params.getDynamics().getSize(buff))
                fct.add(ip.left + ip.lerp)

                while (localInterpos + DIFF < localInterpolator.curveLength) {
                    localInterpos += DIFF.toFloat()
                    ip = localInterpolator.evalExt(localInterpos)

                    buff = PenState(ip.x, ip.y, MUtil.lerp(penStates[ip.left].pressure, penStates[ip.right].pressure, ip.lerp).toFloat())
                    fcx.add(ip.x)
                    fcy.add(ip.y)
                    fcw.add(params.getDynamics().getSize(buff))
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
                    ws[i] = params.getDynamics().getSize(penStates[i])
                    ts[i] = i.toFloat()
                }
                return IndexedDrawPoints(xs, ys, ws, ts)
            }
        }
    }
}
