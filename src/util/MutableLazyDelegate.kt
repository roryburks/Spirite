package util

import kotlin.reflect.KProperty

class MutableLazy<T>(val delegate : () -> T) {
    var field : T? = null

    operator fun getValue(thisRef: Any, prop: KProperty<*>): T {
        val ret = field ?: delegate.invoke()
        field = ret
        return ret
    }

    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {
        field = value
    }
}