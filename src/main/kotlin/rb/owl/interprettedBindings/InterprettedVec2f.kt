package rb.owl.interprettedBindings

import rb.global.IContract
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.vectrix.linear.Vec2f


fun Bindable<Float>.bindToX( root: Bindable<Vec2f> ) : IContract
{
    if( this.field != root.field.xf)
        this.field = root.field.xf

    return InterprettedVec2fBind(this, root, true)
}
fun Bindable<Float>.bindToY( root: Bindable<Vec2f> ) : IContract
{
    if( this.field != root.field.yf)
        this.field = root.field.yf

    return InterprettedVec2fBind(this, root, false)
}

private class InterprettedVec2fBind(
        val bindFloat: Bindable<Float>,
        val bindVec2: Bindable<Vec2f>,
        val x: Boolean) : IContract
{
    private var valueF = 0f
    private var valueV = 0f


    private val k1 =bindVec2.addObserver { new, old ->
        val newf = if( x) new.xf else new.yf
        if( valueF != newf) {
            valueF = newf
            bindFloat.field = valueF
        }
    }
    private val k2 = bindFloat.addObserver { new, old ->
        if( valueV != new) {
            valueV = new
            if( x)
                bindVec2.field = Vec2f(valueV, bindVec2.field.yf)
            else
                bindVec2.field = Vec2f(bindVec2.field.xf, valueV)
        }
    }

    override fun void() {
        k1.void()
        k2.void()
    }
}