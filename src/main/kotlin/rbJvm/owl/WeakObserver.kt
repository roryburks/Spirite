package rbJvm.owl

import rb.IContract
import rb.extendo.dataStructures.SinglySequence
import rb.owl.IObservable
import rb.owl.IObserver
import rb.owl.bindable.IBindable
import java.lang.ref.WeakReference

/** A WeakObserver is an Observer that does not have a strong reference to the trigger.  The idea being that the trigger
 * will have strong references to several potentially-large objects.  Someone else should keep a firm rederence to the
 * trigger.
 */
class WeakObserver<T>(trigger: T) : IObserver<T>
{
    val description = trigger.toString()
    private val weakTrigger = WeakReference(trigger)
    override val triggers : Sequence<T>? get() =   weakTrigger.get()?.run { SinglySequence(this) }
            ?: null.also{ println("$description fallen out of workspace.")}
}

/** Note: the contract strongly references the Trigger, so the t is preserved as long as the contract is. */
fun <T> IObservable<T>.addWeakObserver(t: T) : IContract =
        WeakObserverContract(this.addObserver(WeakObserver(t)), t)

/** Note: the contract strongly references the Trigger, so the trigger is preserved as long as the contract is. */
fun <T> IBindable<T>.addWeakObserver(t: (new: T, old: T)->Unit) : IContract =
        WeakObserverContract(this.addObserver(WeakObserver(t)),t)

private class WeakObserverContract<T>(private val bindContract: IContract, t: T) : IContract {
    var t: T? = t
    override fun void() {
        bindContract.void()
        t = null
    }
}

