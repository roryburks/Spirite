package spirite.base.pen.behaviors

import rb.glow.IGraphicsContext
import sgui.core.components.events.MouseEvent.MouseButton
import spirite.base.pen.Penner
import spirite.gui.views.work.WorkSectionView


// By design, PennerBehaviors has and should make use of all variables local to penner
abstract class PennerBehavior(
        val penner: Penner)
{
    abstract fun onStart()
    abstract fun onTock()
    abstract fun onMove()

    // For most StateBehavior, onPenDown will be irrelevant/not make sense
    //	because their penUp action is to cancel the state.
    open fun onPenDown(button: MouseButton) {}
    open fun onPenUp() {end()}
    open fun onEnd() {}

    fun end() {
        // This effectively ends the state behavior
        penner.behavior = null
    }
}


abstract class DrawnPennerBehavior(penner: Penner) : PennerBehavior(penner) {

    override fun onEnd() {
        this.penner.context.redraw()
    }

    abstract fun paintOverlay(gc: IGraphicsContext, view: WorkSectionView)
}