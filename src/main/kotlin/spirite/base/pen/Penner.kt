package spirite.base.pen

import rb.owl.bindable.addObserver
import rb.vectrix.linear.Vec2f
import rb.vectrix.mathUtil.f
import rb.vectrix.mathUtil.floor
import spirite.base.brains.palette.IPaletteManager
import spirite.base.brains.toolset.*
import spirite.base.brains.toolset.FlipMode.*
import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.rendering.IRenderEngine
import spirite.base.imageData.drawer.IImageDrawer.*
import spirite.base.imageData.selection.ISelectionEngine.BuildMode.*
import spirite.base.pen.behaviors.*
import spirite.base.util.Colors
import spirite.gui.components.basic.events.MouseEvent.MouseButton
import spirite.gui.components.basic.events.MouseEvent.MouseButton.LEFT
import spirite.gui.views.work.WorkSection
import spirite.gui.views.work.WorkSectionView
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
    fun rawUpdatePressure(rawPressure: Float)

    val drawsOverlay : Boolean
    fun drawOverlay(gc: GraphicsContext, view: WorkSectionView)

}


class Penner(
        val context: WorkSection,
        val toolsetManager: IToolsetManager,
        val renderEngine: IRenderEngine,
        val paletteManager: IPaletteManager)
    : IPenner
{
    val holdingSpace get() = Hybrid.keypressSystem.holdingSpace
    var holdingShift = false
    var holdingAlt = false
    var holdingCtrl = false

    val workspace get() = context.currentWorkspace

    var rawX = 0 ; private set
    var rawY = 0 ; private set
    var oldRawX = 0 ; private set
    var oldRawY = 0 ; private set

    var x = 0; private set
    var y = 0; private set
    var xf = 0f; private set
    var yf = 0f; private set

    var oldX = 0 ; private set
    var oldY = 0 ; private set

    var pressure = 1.0f ; private set

    var behavior : PennerBehavior? = null
        set(new) {
            val old = field
            field = new
            old?.onEnd()
            new?.onStart()
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
            context.currentWorkspace?.referenceManager?.editingReference ?: false -> behavior = when {
                holdingCtrl     -> ZoomingReferenceBehavior(this)
                holdingShift    -> RotatingReferenceBehavior( this)
                else            -> MovingReferenceBehavior(this)
            }
            else -> {
                val tool = toolsetManager.selectedTool
                val color = paletteManager.activeBelt.getColor(if( button == LEFT) 0 else 1)
                val offColor = paletteManager.activeBelt.getColor(if( button == LEFT) 1 else 0)

                when {
                    holdingSpace -> context.currentView?.also { behavior =  MovingViewBehavior(this,it )}
                    tool is Pen -> when {
                        holdingCtrl -> behavior = PickBehavior( this, button == LEFT)
                        drawer is IStrokeModule -> behavior = PenBehavior.Stroke(this, drawer, color)
                        else -> Hybrid.beep()
                    }
                    tool is Pixel -> when {
                        holdingCtrl -> behavior = PickBehavior( this, button == LEFT)
                        drawer is IStrokeModule -> behavior = PixelBehavior.Stroke( this, drawer, color)
                        else -> Hybrid.beep()
                    }
                    tool is Eraser ->
                        if( drawer is IStrokeModule) behavior = EraserBehavior.Stroke( this, drawer, color)
                        else Hybrid.beep()
                    tool is Fill ->
                        if( drawer is IFillModule) drawer.fill(x, y, if(holdingCtrl) Colors.TRANSPARENT else color)
                        else Hybrid.beep()
                    tool is Move -> {
                        val selected = workspace.groupTree.selectedNode
                        when {
                            workspace.selectionEngine.selection != null  -> behavior = MovingSelectionBehavior(this)
                           selected != null -> behavior = MovingNodeBehavior(this, selected)
                        }
                    }

                    tool is ShapeSelection ||
                    tool is FreeSelection-> {
                        if(!holdingShift && !holdingCtrl && workspace.selectionEngine.selection?.contains(x,y) == true) {
                            behavior = MovingSelectionBehavior(this)
                        }
                        else {
                            val mode = when {
                                holdingCtrl && holdingShift -> INTERSECTION
                                holdingShift -> ADD
                                holdingCtrl -> SUBTRACT
                                else -> DEFAULT
                            }
                            when(tool) {
                                is ShapeSelection -> behavior = FormingSelectionBehavior(this, toolsetManager.toolset.ShapeSelection.shape, mode)
                                is FreeSelection -> behavior = FreeformSelectionBehavior(this, mode)
                            }
                        }
                    }
                    tool is ColorChanger ->
                        if( drawer is IColorChangeModule) drawer.changeColor( color, offColor, toolsetManager.toolset.ColorChanger.mode)
                        else Hybrid.beep()
                    tool is Flip -> when {
                        drawer !is IFlipModule -> Hybrid.beep()
                        tool.flipMode == HORIZONTAL -> drawer.flip(true)
                        tool.flipMode == VERTICAL -> drawer.flip(false)
                        tool.flipMode == BY_MOVEMENT -> behavior = FlippingBehavior(this, drawer)
                    }
                    tool is Reshaper -> {
                        if( drawer is ITransformModule) behavior = ReshapingBehavior(this, drawer)
                        else Hybrid.beep()
                    }
                    tool is ColorPicker -> behavior = PickBehavior( this, button == LEFT)
                    tool is Rigger -> behavior = RigSelectionBehavior(this, toolsetManager.toolset.Rigger.scope)
                    tool is MagneticFillTool -> {
                        if(drawer is IMagneticFillModule) {
                            behavior = MagneticFillBehavior(this, drawer, color)
                        }
                        else Hybrid.beep()
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
        if( this.rawX != rawX) {
            this.rawX = rawX
            val p = context.currentView?.tScreenToWorkspace?.apply(Vec2f(rawX.f, rawY.f))
            if( p != null) {
                xf = p.xf
                yf = p.yf
                x = p.xf.floor
                y = p.yf.floor
            }
        }
    }

    override fun rawUpdateY(rawY: Int) {
        if( this.rawY != rawY) {
            this.rawY = rawY
            val p = context.currentView?.tScreenToWorkspace?.apply(Vec2f(rawX.f, rawY.f))
            if( p != null) {
                xf = p.xf
                yf = p.yf
                x = p.xf.floor
                y = p.yf.floor
            }
        }
    }

    override fun rawUpdatePressure(rawPressure: Float) { pressure = rawPressure }

    override val drawsOverlay: Boolean get() = behavior is DrawnPennerBehavior
    override fun drawOverlay(gc: GraphicsContext, view: WorkSectionView) {
        (behavior as? DrawnPennerBehavior)?.paintOverlay(gc,view)
    }

    val _toolBindingK = toolsetManager.selectedToolBinding.addObserver { _, _ -> behavior = null }
}