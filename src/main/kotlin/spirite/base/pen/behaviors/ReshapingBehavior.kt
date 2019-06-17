package spirite.base.pen.behaviors

import com.hackoeur.jglm.support.FastMath.max
import rb.glow.color.Colors
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.owl.observer
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.linear.MutableTransformF
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.f
import rbJvm.owl.bindWeaklyTo
import sgui.generic.components.events.MouseEvent.MouseButton
import rb.glow.GraphicsContext
import spirite.base.imageData.drawer.IImageDrawer.ITransformModule
import spirite.base.imageData.selection.ISelectionEngine.SelectionChangeEvent
import spirite.base.pen.Penner
import spirite.base.pen.behaviors.TransformBehavior.TransformStates.*
import spirite.base.util.linear.Rect
import rb.vectrix.shapes.IShape
import rbJvm.vectrix.shapes.OvalShape
import rbJvm.vectrix.shapes.RectShape
import spirite.gui.views.work.WorkSectionView
import kotlin.math.atan2

/***
 * TransformBehavior is an abstract Behavior that encapsulates the drawing and manipulation of the GridUI based on a
 * region (manipulated, presumably, by the derived).
 */
abstract class TransformBehavior( penner: Penner) : DrawnPennerBehavior(penner) {
    enum class TransformStates {
        READY, ROTATE, RESIZE, MOVING, INACTIVE
    }

    // The Calculation ITransformF is a snapshot, taken when switching states:
    // The snapshot is of the transformation from image space to a space where -1,-1 is the lower-left corner of the
    //  transformed [region] Rect using the workingTransform and 1,1 is the upper-right
    private var calcTrans: ITransformF = ImmutableTransformF.Identity
    private fun updateCalcTrans() {
        val region = region ?: return
        val trans = MutableTransformF.Translation(-region.width/2f, -region.height/2f)
        trans.preConcatenate(workingTransform)
        trans.preTranslate(region.width/2f + region.x, region.height/2f + region.y)
        val trans2 = trans.invert()?.toMutable() ?: MutableTransformF.Identity
        trans2.preScale(1/region.width.f, 1f / region.height.f)
        trans2.preTranslate(-0.5f,-0.5f)
        trans2.preScale(2f,2f)
        calcTrans = trans2
    }

    var state = TransformStates.READY
        set(value) {
            when( value) {
                RESIZE -> {
                    startX = penner.x
                    startY = penner.y
                    updateCalcTrans()
                    oldScaleX = scale.xf
                    oldScaleY = scale.yf
                }
                ROTATE -> {
                    startX = penner.x
                    startY = penner.y
                    updateCalcTrans()
                    oldRot = rotation
                }
                else -> {}
            }
            field = value
        }

    private var startX = 0
    private var startY = 0
    private var oldScaleX = 0f
    private var oldScaleY = 0f
    private var oldRot = 0f

    protected val scaleBind = Bindable(Vec2f(1f, 1f))
    protected var scale by scaleBind

    val translationBind = Bindable(Vec2f(0f, 0f))
    var translation by translationBind

    val rotationBind = Bindable(0f)
    var rotation by rotationBind

    var region : Rect? = null

    val workingTransform : ITransformF
        get() {
            val trans = MutableTransformF.Scale(scale.xf, scale.yf)
            trans.preRotate(rotation)
            trans.preTranslate(translation.xf, translation.yf)
            return trans
        }

    var overlap = -1

    fun calcDisplayTransform( view : WorkSectionView) : ITransformF {
        val region = region ?: return workingTransform
        val trans = MutableTransformF.Translation(-region.width/2f, -region.height/2f)
        trans.preConcatenate(workingTransform)
        trans.preTranslate(region.width/2f + region.x, region.height/2f + region.y)
        trans.preScale( view.zoom, view.zoom)
        val orig = view.tWorkspaceToScreen.apply(Vec2f.Zero)
        trans.preTranslate(orig.xf, orig.yf)
        return trans
    }




    override fun paintOverlay(gc: GraphicsContext, view: WorkSectionView) {
        val region = region ?: return
        if( region.isEmpty || state == INACTIVE) return

        val zoom = view.zoom

        gc.pushTransform()

        val relTrans = calcDisplayTransform(view)
        gc.transform = relTrans

        val w = region.width
        val h = region.height
        gc.color = Colors.BLACK
        gc.drawRect(0, 0, w, h)

//			Stroke defStroke = new BasicStroke( 2/zoom);
        gc.color = Colors.GRAY
//			gc.setStroke(defStroke);

        val p = relTrans.invert()?.apply(Vec2f(penner.rawX.f, penner.rawY.f)) ?: Vec2f(0f,0f)

        val sw = w*0.3f // Width of corner Rect
        val sh = h*0.3f // Height "
        val x2 = w * 0.7f // Offset of right Rect
        val y2 = h * 0.7f // " bottom
        val di = max(0.2f, 10f)  // Radius of circle
        val of = h*0.25f*0.2f

        val b = 1/zoom

        val shapes = mutableListOf<IShape>()
        shapes.add(RectShape(sw + b, b, x2 - sw - b * 2, sh - b * 2))	// N
        shapes.add(RectShape(x2 + b, sh + b, sw - b * 2, y2 - sh - b * 2))// E
        shapes.add(RectShape(sw + b, y2 + b, x2 - sw - b * 2, sh - b * 2))// S
        shapes.add(RectShape(0 + b, sh + b, sw - b * 2, y2 - sh - b * 2))	// W

        shapes.add(RectShape(b, b, sw - b * 2, sh - b * 2))			// NW
        shapes.add(RectShape(x2 + b, b, sw - b * 2, sh - b * 2))		// NE
        shapes.add(RectShape(x2 + b, y2 + b, sw - b * 2, sh - b * 2))	// SE
        shapes.add(RectShape(b, y2 + b, sw - b * 2, sh - b * 2))		// SW

        shapes.add(OvalShape(-of, -of, di, di))	// NW
        shapes.add(OvalShape(w + of, -of, di, di))	// NE
        shapes.add(OvalShape(w + of, h + of, di, di))	// SE
        shapes.add(OvalShape(-of, h + of, di, di))	// SW

        shapes.add(RectShape(sw + b, sh + b, x2 - sw - b * 2, y2 - sh - b * 2))	// Center

        gc.alpha = 0.5f

        if( state == READY)
            overlap = -1
        shapes.forEachIndexed { i, shape ->
            if( overlap == i || (overlap == -1 && shape.contains(p.xf, p.yf))) {
                gc.color = Colors.YELLOW
//					gc.setStroke(new BasicStroke( 4/zoom));

                gc.draw(shape)
                gc.color = Colors.GRAY
                overlap = i
            }
            else gc.draw(shape)
        }
        gc.alpha = 1f

        gc.popTransform()
    }

    override fun onMove() {
        when( state) {
            MOVING -> if( penner.oldX != penner.x || penner.oldY != penner.y)
                translation = Vec2f(translation.xf + penner.x - penner.oldX, translation.yf + penner.y - penner.oldY)
            RESIZE -> {
                val pn = calcTrans.apply(Vec2f(penner.x.f, penner.y.f))
                val ps = calcTrans.apply(Vec2f(startX.f, startY.f))
                val sx = if (overlap == 0 || overlap == 2) scale.xf else pn.xf / ps.xf * oldScaleX
                val sy = if (overlap == 1 || overlap == 3) scale.yf else pn.yf / ps.yf * oldScaleY

                // Preserve Ratio
                scale =  scalePreservingRatio(scale, sx, sy)

                        when {
                    penner.holdingShift -> scalePreservingRatio(scale, sx, sy)
                    else -> Vec2f(sx,sy)
                }
            }
            ROTATE -> {
                val pn = calcTrans.apply(Vec2f(penner.x.f, penner.y.f))
                val ps = calcTrans.apply(Vec2f(startX.f,startY.f))

                val start = atan2(ps.yf, ps.xf)
                val end = atan2(pn.yf, pn.xf)
                rotation = end - start + oldRot
            }
            else ->{}
        }
    }

    fun scalePreservingRatio(old: Vec2f, sx: Float, sy: Float) : Vec2f {
        val moveX = when {
            sx == 0f && old.xf == 0f -> 0f
            sx < old.xf -> old.xf / sx
            else -> sx / old.xf
        }
        val moveY = when {
            sy == 0f && old.yf == 0f -> 0f
            sy < old.yf -> old.yf / sy
            else -> sy / old.yf
        }

        // Scale
        return when {
            moveX > moveY -> Vec2f(sx, old.yf * sx / old.xf)
            else -> Vec2f(old.xf * sy / old.yf, sy)
        }
    }
}

/***
 * ReshapingBehavior derives from TransformBehavior, which handles all the transforming/drawing logic, whereas
 * ReshapingBehavior binds the transform being manipulated to the ReshapingTool's transform stats and _triggers a
 * ITranformModule drawer.
 */
class ReshapingBehavior(penner: Penner, var drawer: ITransformModule) : TransformBehavior(penner)
{
    val tool = penner.toolsetManager.toolset.Reshape
    val workspace = penner.workspace

    private val _scaleK = scaleBind.bindWeaklyTo(tool.scaleBind)
    private val _transK = translationBind.bindWeaklyTo(tool.translationBind)
    private val _rotK = rotationBind.bindWeaklyTo(tool.rotationBind)

    private val _link1 = tool.scaleBind.addObserver { _, _ -> onChange()}
    private val _link2 = tool.translationBind.addObserver { _, _ -> onChange()}
    private val _link3 = tool.rotationBind.addObserver { _, _ -> onChange()}
    private val _link4 = penner.workspace?.selectionEngine?.selectionChangeObserver?.addObserver({ it: SelectionChangeEvent ->
        end()
    }.observer())
    private val _link5 = tool.applyTransformBind.addObserver { _, _ -> tryStart() }


    private fun onChange() {
        drawer.stepManipulatingTransform()
    }

    override fun onStart() {
        if(!tryStart())
            end()
    }

    private fun tryStart() : Boolean {
        val workspace = workspace?: return false

        region = drawer.startManipulatingTransform() ?: kotlin.run {
            drawer = workspace.activeDrawer as? ITransformModule ?: return false
            drawer.startManipulatingTransform() ?: return false
        }

        return true
    }

    override fun onEnd() {
        _link1.void()
        _link2.void()
        _link3.void()
        _link4?.void()
        _link5.void()
        _rotK.void()
        _transK.void()
        _scaleK.void()

        scale = Vec2f(1f,1f)
        translation = Vec2f(0f,0f)
        rotation = 0f

        drawer.endManipulatingTransform()


        super.onEnd()
    }

    override fun onTock() {}
    override fun onPenUp() {
        state = READY
    }

    override fun onPenDown(button: MouseButton) {
        state = when( overlap) {
            in 0..7 -> RESIZE
            in 8..0xB -> ROTATE
            0xC -> MOVING
            else -> state
        }
    }
}