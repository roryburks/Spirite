package rb.owl.bindable

import rb.global.IContract

// A PushPullBind operates for most functionality purposes as a Binding between two Bindables, but instead of pooling
//  themselves into a single Underlying, they maintain a push-pull relationship, where one pushes changes to the other.
//  This is particularly useful when layers of separation are needed for more complicated Bindings.
// NOTE: on creation the left will inherit the value of the right similar to left.bindTo(right)
class PushPullBind<T>(
    left : Bindable<T>,
    right : Bindable<T> ) : IContract
{
    init { left.field = right.field }

    val leftK = left.addObserver { new, _ -> right.field = new  }
    val rightK = right.addObserver { new, _ -> left.field = new  }

    override fun void() {
        leftK.void()
        rightK.void()
    }
}