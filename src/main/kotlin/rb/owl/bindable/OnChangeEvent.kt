package rb.owl.bindable

import rb.extendo.dataStructures.SinglySequence


typealias OnChangeEvent<T> = (new: T, old:T)->Unit

fun <T> onChangeObserver(trigger: (new:T, old:T)->Unit ) = object : IBindObserver<T> {
    override val triggers = SinglySequence(trigger)
}

fun <T> IBindable<T>.addObserver(trigger: Boolean = true, event: (new: T, old: T) -> Unit)
        = addObserver(onChangeObserver<T>(event), trigger)