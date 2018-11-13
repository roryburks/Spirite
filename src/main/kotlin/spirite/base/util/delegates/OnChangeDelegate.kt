package spirite.base.util.delegates

import kotlin.reflect.KProperty


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