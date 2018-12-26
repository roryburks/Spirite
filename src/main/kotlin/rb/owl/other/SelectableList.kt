package rb.owl.other

import rb.owl.IObservable
import rb.owl.IObserver
import rb.owl.Observable
import rb.owl.bindable.Bindable
import rb.owl.bindable.IBindable
import rb.owl.bindable.addObserver


interface ISelectableListTriggers<T> {
    fun created( t: T)
    fun removed( t: T)
    fun changed( new: T?, old: T?)
}

interface ISelectableList<T> :IObservable<ISelectableListTriggers<T>> {
    val currentBind: IBindable<T?>
    val current : T?
    val all : List<T>
}
interface MSelectableList<T> : ISelectableList<T> {
    override val currentBind: Bindable<T?>
    override var current : T?
    fun add(t: T, select: Boolean = false)
    fun remove( t: T)
}

class SelectableList<T> :  MSelectableList<T> {

    private val _list = mutableListOf<T>()
    override val currentBind = Bindable<T?>(null)
    override var current by currentBind
    override val all: List<T> get() = _list

    override fun add(t: T, select: Boolean) {
        _list.add(t)
        underlying.trigger { it.created(t) }
        if( select || current == null) {
            current = t
        }
    }

    override fun remove(t: T) {
        if( t == current ) {
            val index = _list.indexOf(t)
            current = _list.getOrNull(index-1) ?: _list.getOrNull(index + 1)
        }
        _list.remove(t)
        underlying.trigger { it.removed(t) }
    }

    private val underlying = Observable<ISelectableListTriggers<T>>()
    override fun addObserver(observer: IObserver<ISelectableListTriggers<T>>, trigger: Boolean) =
        underlying.addObserver(observer, trigger)


    init {
        currentBind.addObserver { new, old ->underlying.trigger { it.changed(new, old) }}
    }
}