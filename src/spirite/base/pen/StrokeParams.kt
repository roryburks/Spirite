package spirite.base.pen

import spirite.base.brains.tools.ToolSchemes.PenDrawMode
import spirite.base.brains.tools.ToolSchemes.PenDrawMode.NORMAL
import spirite.base.pen.PenTraits.PenDynamics
import spirite.base.pen.PenTraits.PenState
import spirite.base.pen.StrokeEngine.Method
import spirite.base.pen.StrokeEngine.Method.BASIC
import spirite.base.util.Colors
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

/**
 * StrokeParams define the style/tool/options of the Stroke.
 *
 * lock is not actually used yet, but changing data mid-stroke is a
 * bar idea.
 */
class StrokeParams {
    var color by LockingDelegate(Colors.BLACK)
    var method by LockingDelegate( Method.BASIC)
    var mode by LockingDelegate(PenDrawMode.NORMAL)
    var width by LockingDelegate( 1.0f)
    var alpha by LockingDelegate( 1.0f)
    var dynamics by LockingDelegate( PenDynamicsConstants.getBasicDynamics())
    var interpolationMethod by LockingDelegate( InterpolationMethod.CUBIC_SPLINE)
    var hard by LockingDelegate(false)
    var maxWidth by LockingDelegate(25)




    /** If Params are locked, they're being used and can't be changed.
     * Only the base StrokeEngine can lock/unlock Params.  Once they are
     * locked they will usually never be unlocked as the UndoEngine needs
     * to remember the saved settings.
     */
    var locked = false

    inner class LockingDelegate<T> (
            var field: T
    ){
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
            if( !locked)
                field = newValue
        }
    }


    enum class InterpolationMethod {
        NONE,
        CUBIC_SPLINE
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
            out.color = original.color
            out.dynamics = PenDynamicsConstants.LinearDynamics()
            out.hard = original.hard
            out.interpolationMethod = original.interpolationMethod
            out.method = original.method
            out.mode = original.mode
            out.width = original.width

            for (i in penStates.indices) {
                penStates[i] = PenState(penStates[i].x, penStates[i].y,
                        original.dynamics.getSize(penStates[i]))
            }

            return out
        }
    }
}