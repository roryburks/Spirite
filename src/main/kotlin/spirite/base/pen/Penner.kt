package spirite.base.pen

import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.toolset.*
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.drawer.IImageDrawer.IFillModule
import spirite.base.imageData.drawer.IImageDrawer.IStrokeModule
import spirite.base.pen.behaviors.*
import spirite.base.util.delegates.DerivedLazy
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.linear.Vec2
import spirite.gui.components.basic.events.MouseEvent.MouseButton
import spirite.gui.components.basic.events.MouseEvent.MouseButton.LEFT
import spirite.gui.components.major.work.WorkSection
import spirite.hybrid.Hybrid

interface IPenner {
//    val holdingShift : Boolean
//    val holdingAlt  : Boolean
//    val holdingCtrl : Boolean

    fun step()
    fun penDown(button: MouseButton)
    fun penUp(button: MouseButton)
    fun reset()

    fun rawUpdateX(rawX: Int)
    fun rawUpdateY(rawY: Int)
    fun rawUpdatePressure(pressure: Float)

}


class Penner(
        val context: WorkSection,
        val toolsetManager: IToolsetManager,
        val renderEngine: IRenderEngine,
        val paletteManager: IPaletteManager)
    : IPenner
{

    var holdingShift = false
    var holdingAlt = false
    var holdingCtrl = false

    val workspace get() = context.currentWorkspace


    var rawX = 0 ; private set
    var rawY = 0 ; private set
    var oldRawX = 0 ; private set
    var oldRawY = 0 ; private set

    private val xDerived : DerivedLazy<Int> = DerivedLazy {
        val p = context.currentView?.tScreenToWorkspace?.apply(Vec2(rawX.f, rawY.f))
        yDerived.field = p?.y?.floor ?: rawY
        p?.x?.floor ?: rawX
    }
    private val yDerived : DerivedLazy<Int> = DerivedLazy {
        val p = context.currentView?.tScreenToWorkspace?.apply(Vec2(rawX.f, rawY.f))
        xDerived.field = p?.x?.floor ?: rawX
        p?.y?.floor ?: rawY
    }
    val x by xDerived
    var y by yDerived
    var oldX = 0 ; private set
    var oldY = 0 ; private set


    var pressure = 1.0f ; private set

    var behavior : PennerBehavior? = null
        set(new) {
            val old = field
            field = new
            if( old != null)
                old.onEnd()
            if( new != null)
                new.onStart()
        }

    init {
        context.currentView
    }

    override fun step() {
        if( oldX != x || oldY != y) {
            behavior?.onMove()
            if( behavior is DrawnPennerBehavior)
                context.redraw()
            context.refreshCoordinates(x, y)
        }

        behavior?.onTock()

        oldX = x
        oldY = y
        oldRawX = rawX
        oldRawY = rawY
    }

    override fun penDown(button: MouseButton) {
        if( button == MouseButton.UNKNOWN) return

        // There really shouldn't be any reason to do penDown behavior when workspace is null
        val workspace = workspace ?: return
        val drawer = workspace.activeDrawer

        when {
            behavior != null -> behavior?.onPenDown()
            context.currentWorkspace?.referenceManager?.editingReference ?: false -> when {
                holdingCtrl ->  behavior = ZoomingReferenceBehavior(this)
                holdingShift -> behavior = RotatingReferenceBehavior( this)
                else ->         behavior = MovingReferenceBehavior(this)
            }
            else -> {
                val tool = toolsetManager.selectedTool
                val color = paletteManager.getActiveColor(if( button == LEFT) 0 else 1)

                when( tool) {
                    is Pen -> when {
                        holdingCtrl -> behavior = PickBehavior( this, button == LEFT)
                        drawer is IStrokeModule -> behavior = PenBehavior.Stroke(this, drawer, color)
                        else -> Hybrid.beep()
                    }
                    is Pixel -> when {
                        holdingCtrl -> behavior = PickBehavior( this, button == LEFT)
                        drawer is IStrokeModule -> behavior = PixelBehavior.Stroke( this, drawer, color)
                        else -> Hybrid.beep()
                    }
                    is Eraser ->
                        if( drawer is IStrokeModule) behavior = EraserBehavior.Stroke( this, drawer, color)
                        else Hybrid.beep()
                    is Fill ->
                        if( drawer is IFillModule) TODO()
                        else Hybrid.beep()
                    is Move -> when {
                        //workspace.selectionEngine.selection != null  -> TODO()
                        workspace.groupTree.selectedNode != null -> behavior = MovingNodeBehavior(this, workspace.groupTree.selectedNode!!)
                    }

                }

            }
        }
    }

    override fun penUp(button: MouseButton) {
        behavior?.onPenUp()
    }

    override fun reset() {
        behavior = null
        context.redraw()
    }

    override fun rawUpdateX(rawX: Int) {
        this.rawX = rawX
        xDerived.reset()
        yDerived.reset()
    }

    override fun rawUpdateY(rawY: Int) {
        this.rawY = rawY
        xDerived.reset()
        yDerived.reset()
    }

    override fun rawUpdatePressure(rawPressure: Float) {
        pressure = rawPressure
    }
}