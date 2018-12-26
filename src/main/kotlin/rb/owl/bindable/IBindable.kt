package rb.owl.bindable

import rb.owl.IObservable
import rb.owl.IObserver


typealias IBindObserver<T> = IObserver<OnChangeEvent<T>>

interface IBindable<T> : IObservable<OnChangeEvent<T>> {
    val field: T
}