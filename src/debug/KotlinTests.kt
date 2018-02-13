package debug

import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty


fun main( args: Array<String>) {
    val listlist = listOf(
            listOf(1, 2, 3),
            listOf(1, 2, 5),
            listOf(1, 7, 8)
    )

    println(
            listlist
                    .reduce { acc, list ->  acc.union(list).toList()})

}

//region Test 1
fun kt001_test() {
    // Demonstrates how a masking Property can be Delegated in Kotlin
    val td = kt001_testDelegates( kt001_testDelegates.Structure(1,0.5f, "Huff"))

    System.out.println(td.prop1)
    System.out.println(td.prop2)
    System.out.println(td.prop3)
    td.prop1 = 2
    td.prop2 = 5.5f
    td.prop3 = "Tuft"
    System.out.println(td.prop1)
    System.out.println(td.prop2)
    System.out.println(td.prop3)
    td.wipe()
    System.out.println(td.prop1)
    System.out.println(td.prop2)
    System.out.println(td.prop3)
}
class kt001_testDelegates (
        private val structure: Structure
){
    data class Structure(
            var a : Int,
            var b: Float,
            var c: String
    )

    var prop1 : Int by Delegate( structure::a)
    var prop2 : Float by Delegate( structure::b)
    var prop3 : String by Delegate( structure::c)

    fun wipe() {
        structure.a = 0
        structure.b = 0f
        structure.c = "-"
    }

    inner class Delegate<T> ( val innerprop: KMutableProperty<T>){
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return innerprop.getter.call()
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            innerprop.setter.call(value)
        }
    }
}
//endregion


// region Test 2

fun k002_testPropertyLev() {
    // Demonstrates how to properly have a dynamic reference property
    val a = k002_A()
    a.b.number = 5
    System.out.println(a.a1)    // 5
    System.out.println(a.a2)    // 0
}
class k002_A {
    var b = k002_B()
    val a1: Int get() = b.number
    val a2 = b.number
}
class k002_B {
    var number = 0
}

// endregion

// region Test3

fun k003_testDelegates() {
    var a = k003_A()

    a.pass = "test"
    System.out.println(a.pass)
    a.locked = true
    a.pass = " bad"
    System.out.println(a.pass)
}
class k003_A {
    var pass : String by LockingDelegate("")


    var locked = false

    inner class LockingDelegate<T> (
            var field: T
    ){
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
            if( !locked)
                field = newValue
        }
    }
}

// endregion