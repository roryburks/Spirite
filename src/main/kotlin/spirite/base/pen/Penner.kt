package spirite.base.pen

import spirite.base.pen.behaviors.DrawnPennerBehavior
import spirite.base.pen.behaviors.PennerBehavior
import spirite.base.util.delegates.DerivedLazy
import spirite.base.util.f
import spirite.base.util.floor
import spirite.base.util.linear.Vec2
import spirite.gui.major.work.WorkSection

interface IPenner {
//    val holdingShift : Boolean
//    val holdingAlt  : Boolean
//    val holdingCtrl : Boolean

    fun step()
    fun penDown()
    fun penUp()
    fun reset()

    fun rawUpdateX(rawX: Int)
    fun rawUpdateY(rawY: Int)
    fun rawUpdatePressure(pressure: Float)

}


class Penner(
        val context: WorkSection)
    : IPenner
{

    var holdingShift = false ; private set
    var holdingAlt = false ; private set
    var holdingCtrl = false ; private set


    var rawX = 0 ; private set
    var rawY = 0 ; private set
    //var oldRawX = 0 ; private set
    //var oldRawY = 0 ; private set

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
        //oldRawX = rawX
        //oldRawY = rawY
    }

    override fun penDown() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun penUp() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reset() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun rawUpdatePressure(pressure: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}