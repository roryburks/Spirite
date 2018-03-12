package spirite.base.util.delegates

import kotlin.reflect.KProperty


//class DerivedLazy<T>(val delegate : () -> T, val getter: () -> T?) {
//    var field : T? = null
//
//    operator fun getValue(thisRef: Any, prop: KProperty<*>): T {
//        val
//        val ret = field ?: delegate.invoke()
//        field = ret
//        return ret
//    }
//
//    operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {
//        field = value
//    }
//}