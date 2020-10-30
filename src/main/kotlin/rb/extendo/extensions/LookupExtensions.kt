package rb.extendo.extensions

typealias Lookup<Key,Value> = Map<Key,List<Value>>
typealias MLookup<Key,Value> = MutableMap<Key,MutableList<Value>>

fun <Key,Value> MLookup<Key,Value>.append(key: Key, value: Value) {
    (this[key] ?: mutableListOf<Value>().also{this[key] = it}).add(value)
}
fun <Key,Value> Lookup<Key,Value>.lookup(key: Key) : List<Value> = this[key] ?: emptyList()

fun <Key,Value> MLookup<Key,Value>.deref(key: Key, value: Value) = this[key]?.remove(value) ?: false

fun <Key,Id,Value> MutableMap<Key,HashMap<Id,Value>>.append( key: Key, id: Id, value: Value) {
    (this[key] ?: hashMapOf<Id,Value>().also{this[key] = it})[id] = value
}


fun <Key,Value> Sequence<Value>.toLookup( selector: (Value)->Key)
        = mutableMapOf<Key,MutableList<Value>> ()
    .also{map -> forEach { map.append(selector(it),it) }}

fun <Key,Value> Iterable<Value>.toLookup( selector: (Value)->Key)
        = mutableMapOf<Key,MutableList<Value>> ()
    .also{map -> forEach { map.append(selector(it),it) }}

fun <Key,Base,Value> Iterable<Base>.toLookup( selector: (Base)->Key, transformer: (Base)->Value)
        = mutableMapOf<Key,MutableList<Value>> ()
    .also{map -> forEach { map.append(selector(it),transformer(it)) }}
