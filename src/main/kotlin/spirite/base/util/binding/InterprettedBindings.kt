package spirite.base.util.binding

import spirite.base.brains.Bindable
import spirite.base.brains.MBindable
import spirite.base.util.linear.Vec2


class InterprettedFloatBind(val baseBind : Bindable<Vec2>, val x : Boolean) : MutableBindableStub<Float>() {
    override var field: Float
        get() {
            return if (x) baseBind.field.x else baseBind.field.y
        }
        set(value) {
            val old = if(x) baseBind.field.x else baseBind.field.y
            if( old != value) {
                baseBind.field = when(x) {
                    true -> Vec2(value, baseBind.field.y)
                    false -> Vec2(baseBind.field.x, value)
                }

            }
        }
}

val Bindable<Vec2>.xBind : MBindable<Float> get() = InterprettedFloatBind(this, true)
val Bindable<Vec2>.yBind : MBindable<Float> get() = InterprettedFloatBind(this, false)