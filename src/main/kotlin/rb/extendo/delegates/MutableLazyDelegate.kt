package rb.extendo.delegates

import kotlin.reflect.KProperty

class MutableLazy<T>(private val delegate : () -> T) {
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