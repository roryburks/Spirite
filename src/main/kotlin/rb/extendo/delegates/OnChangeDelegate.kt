package rb.extendo.delegates

import kotlin.reflect.KProperty

// Note: this is similar to Delegates.observable, but this one only triggers its change if the value changes rather than on any set
open class OnChangeDelegate<T>(defaultValue : T, val onChange: (T) -> Unit) {
    var field = defaultValue

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field

    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {
        if( field != value) {
            field = value
            onChange.invoke(value)
        }
    }
}