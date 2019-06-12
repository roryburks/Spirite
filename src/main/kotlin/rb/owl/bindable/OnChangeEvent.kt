package rb.owl.bindable


typealias OnChangeEvent<T> = (new: T, old:T)->Unit

fun <T> onChangeObserver(trigger: (new:T, old:T)->Unit ) = object : IBindObserver<T> {
    override val trigger: OnChangeEvent<T> = trigger
}

fun <T> IBindable<T>.addObserver(trigger: Boolean = true, event: (new: T, old: T) -> Unit)
        = addObserver(onChangeObserver<T>(event), trigger)