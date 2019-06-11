package rb.owl.bindableMList

import rb.owl.bindable.Bindable

interface IBindableMListSet<T> : IBindableMList<T> {
    val currentlySelectedBind: Bindable<T?>
    var currentlySelected : T?
}

class BindableMListSet <T> (
        collection: Collection<T> = emptyList(),
        default: T? = null)
    :IBindableMList<T> by BindableMList<T>(collection), IBindableMListSet<T>
{
    init {
        onRemove { if( it.contains(currentlySelected)) currentlySelected = null }
    }

    override val currentlySelectedBind: Bindable<T?> = Bindable(default)
    override var currentlySelected: T? by currentlySelectedBind
}