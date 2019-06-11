package rb.owl.bindable


typealias IBindObserver<T> = IObserver<OnChangeEvent<T>>

interface IBindable<T> : IObservable<OnChangeEvent<T>> {
    val field: T
}