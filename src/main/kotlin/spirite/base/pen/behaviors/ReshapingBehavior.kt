package spirite.base.pen.behaviors

import com.hackoeur.jglm.support.FastMath.max
import spirite.base.brains.Bindable
import spirite.base.graphics.GraphicsContext
import spirite.base.imageData.drawer.IImageDrawer
import spirite.base.imageData.drawer.IImageDrawer.ILiftSelectionModule
import spirite.base.imageData.mediums.CompositeSource
import spirite.base.pen.Penner
import spirite.base.pen.behaviors.TransformBehavior.TransformStates.*
import spirite.base.util.Colors
import spirite.base.util.MathUtil
import spirite.base.util.f
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Rect
import spirite.base.util.linear.Transform
import spirite.base.util.linear.Transform.Companion
import spirite.base.util.linear.Vec2
import spirite.base.util.shapes.IShape
import spirite.base.util.shapes.Oval
import spirite.base.util.shapes.Rectangle
import spirite.gui.components.major.work.WorkSectionView
import kotlin.math.atan2

abstract class TransformBehavior( penner: Penner) : DrawnPennerBehavior(penner) {
    enum class TransformStates {
        READY, ROTATE, RESIZE, MOVING, INACTIVE
    }

    // The Calculation Transform is a version of the Locked Transform which has
    //	all the relevent offsets built-in so that calculation changes in mouse
    //	movement with respect to the selection's center can be easily performed.
    private var calcTrans: Transform = Transform.IdentityMatrix

    var state = TransformStates.READY
        set(value) {
            when( value) {
                RESIZE -> {
                    startX = penner.x
                    startY = penner.y
                    calcTrans = workingTransform.invert()
                    oldScaleX = scale.x
                    oldScaleY = scale.y
                }
                ROTATE -> {
                    startX = penner.x
                    startY = penner.y
                    calcTrans = workingTransform.invert()
                    oldRot = rotation
                }
                else -> {}
            }
            field = value
        }

    var startX = 0
    var startY = 0
    var oldScaleX = 0f
    var oldScaleY = 0f
    var oldRot = 0f

    val scaleBind = Bindable(Vec2(1f,1f))
    var scale by scaleBind

    val translationBind = Bindable(Vec2(0f,0f))
    var translation by translationBind

    val rotationBind = Bindable(0f)
    var rotation by rotationBind

    var region : Rect? = null

    val workingTransform : Transform get() {
        val trans = MutableTransform.ScaleMatrix(scale.x, scale.y)
        trans.preRotate(rotation)
        trans.preTranslate(translation.x, translation.y)
        return trans
    }

    fun calcDisplayTransform( view : WorkSectionView) : Transform {
        val region = region ?: return workingTransform
        val trans = MutableTransform.TranslationMatrix(-region.width/2f, -region.height/2f)
        trans.preConcatenate(workingTransform)
        trans.preTranslate(region.width/2f + region.x, region.height/2f + region.y)
        trans.preScale( view.zoom, view.zoom)
        val orig = view.tWorkspaceToScreen.apply(Vec2.Zero)
        trans.preTranslate(orig.x, orig.y)
        return trans
    }

    var overlap = -1


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

        val p = relTrans.invert().apply(Vec2(penner.rawX.f, penner.rawY.f))

        val sw = w*0.3f // Width of corner Rect
        val sh = h*0.3f // Height "
        val x2 = w * 0.7f // Offset of right Rect
        val y2 = h * 0.7f // " bottom
        val di = max(0.2f, 10f)/2f  // Radius of circle
        val of = h*0.25f*0.2f

        val b = 1/zoom

        val shapes = mutableListOf<IShape>()
        shapes.add(Rectangle(sw+b, b, x2-sw-b*2, sh-b*2))	// N
        shapes.add(Rectangle(x2+b, sh+b, sw-b*2, y2-sh-b*2))// E
        shapes.add(Rectangle(sw+b, y2+b, x2-sw-b*2, sh-b*2))// S
        shapes.add(Rectangle(0+b, sh+b, sw-b*2, y2-sh-b*2))	// W

        shapes.add(Rectangle(b, b, sw-b*2, sh-b*2))			// NW
        shapes.add(Rectangle(x2+b, b, sw-b*2, sh-b*2))		// NE
        shapes.add(Rectangle(x2+b, y2+b, sw-b*2, sh-b*2))	// SE
        shapes.add(Rectangle(b, y2+b, sw-b*2, sh-b*2))		// SW

        shapes.add(Oval( -of, -of, di, di))	// NW
        shapes.add(Oval( w+of, -of, di, di))	// NE
        shapes.add(Oval( w+of, h+of, di, di))	// SE
        shapes.add(Oval( -of, h+of, di, di))	// SW

        shapes.add(Rectangle(sw+b, sh+b, x2-sw-b*2, y2-sh-b*2))	// Center

        gc.alpha = 0.5f

        if( state == READY)
            overlap = -1
        shapes.forEachIndexed { i, shape ->
            if( overlap == i || (overlap == -1 && shape.contains(p.x, p.y))) {
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
                translation = Vec2(translation.x + penner.x - penner.oldX, translation.y + penner.y - penner.oldY)
            RESIZE -> {
                val pn = calcTrans.apply(Vec2(penner.x.f, penner.y.f))
                val ps = calcTrans.apply(Vec2(startX.f, startY.f))
                val sx = if (overlap == 0 || overlap == 2) scale.x else pn.x / ps.x * oldScaleX
                val sy = if (overlap == 1 || overlap == 3) scale.y else pn.y / ps.y * oldScaleY

                scale = Vec2(sx,sy)
            }
            ROTATE -> {
                val pn = calcTrans.apply(Vec2(penner.x.f, penner.y.f))
                val ps = calcTrans.apply(Vec2(startX.f,startY.f))

                val start = atan2(ps.y, ps.x)
                val end = atan2(pn.y, pn.x)
                rotation = end - start + oldRot
            }
            else ->{}
        }
    }
}

class ReshapingBehavior(penner: Penner, var drawer: IImageDrawer) : TransformBehavior(penner)
{

    val tool = penner.toolsetManager.toolset.Reshape

    init {
        tool.scaleBind.bindWeakly(scaleBind)
        tool.translationBind.bindWeakly(translationBind)
        tool.rotationBind.bindWeakly(rotationBind)
    }

    private val link1 = tool.scaleBind.addListener{_, _ -> onChange()}
    private val link2 = tool.translationBind.addListener{_, _ -> onChange()}
    private val link3 = tool.rotationBind.addListener{_, _ -> onChange()}


    private val linker2 = {_: Float, _: Float -> onChange()}.apply {
        tool.rotationBind.addListener(this)
    }




    private fun onChange() {
        penner.workspace?.compositor?.triggerCompositeChanged()
    }

    override fun onStart() {
        val workspace = penner.workspace?: return
        val selectionEngine = workspace.selectionEngine
        val selected = selectionEngine.selection
        val lifted = selectionEngine.liftedData

        if( selected != null && lifted == null && drawer is ILiftSelectionModule) {
            selectionEngine.attemptLiftData(drawer)
            drawer = workspace.activeDrawer
            val nSelected = selectionEngine.selection
            val nLifted = selectionEngine.liftedData

            if( nLifted != null && nSelected != null) {
                val liftedRect = Rect(nLifted.width, nLifted.height)
                region = if( nSelected.transform == null) liftedRect
                         else MathUtil.circumscribeTrans(Rect(nLifted.width, nLifted.height), nSelected.transform)
            }
        }
        else {
            if( selected != null && lifted == null )
                selectionEngine.setSelection(null)
            val arranged = workspace.activeData ?: return

            workspace.compositor.compositeSource = CompositeSource(arranged,false) {gc ->
                val medium = arranged.handle.medium

                val cx = medium.width /2f
                val cy = medium.height /2f

                val effectiveTrans = Transform.TranslationMatrix(cx,cy) * tool.transform * Transform.TranslationMatrix(-cx,-cy)

                gc.transform = effectiveTrans
                arranged.handle.medium.render(gc)
            }

            region = MathUtil.circumscribeTrans(Rect(arranged.handle.width, arranged.handle.height), arranged.tMediumToWorkspace)
        }
    }

    override fun onEnd() {
        link1.unbind()
        link2.unbind()
        link3.unbind()

        scale = Vec2(1f,1f)
        translation = Vec2(0f,0f)
        rotation = 0f

        super.onEnd()
    }

    override fun onTock() {}
    override fun onPenUp() {
        state = READY
    }

    override fun onPenDown() {
        state = when( overlap) {
            in 0..7 -> RESIZE
            in 8..0xB -> ROTATE
            0xC -> MOVING
            else -> state
        }
    }
}