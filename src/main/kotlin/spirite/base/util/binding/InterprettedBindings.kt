package spirite.base.util.binding

import rb.vectrix.linear.Vec2f


class InterprettedFloatBind(val baseBind : CruddyBindable<Vec2f>, val x : Boolean) : MutableBindableStub<Float>() {
    override var field: Float
        get() {
            return if (x) baseBind.field.xf else baseBind.field.yf
        }
        set(value) {
            val old = if(x) baseBind.field.xf else baseBind.field.yf
            if( old != value) {
                baseBind.field = when(x) {
                    true -> Vec2f(value, baseBind.field.yf)
                    false -> Vec2f(baseBind.field.xf, value)
                }

            }
        }
}

val CruddyBindable<Vec2f>.xBind : MCruddyOldBindable<Float> get() = InterprettedFloatBind(this, true)
val CruddyBindable<Vec2f>.yBind : MCruddyOldBindable<Float> get() = InterprettedFloatBind(this, false)