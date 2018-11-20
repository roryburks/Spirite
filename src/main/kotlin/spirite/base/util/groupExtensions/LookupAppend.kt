package spirite.base.util.groupExtensions

fun <Key,Value> MutableMap<Key,MutableList<Value>>.append( key: Key, value: Value) {
    (this[key] ?: mutableListOf<Value>().also{this[key] = it}).add(value)
}

fun <Key,Value> Map<Key,List<Value>>.lookup( key: Key) : List<Value> = this[key] ?: emptyList()

fun <Key,Value> Sequence<Value>.toLookup( selector: (Value)->Key)
        = mutableMapOf<Key,MutableList<Value>> ()
        .also{map -> forEach { map.append(selector(it),it) }}

fun <Key,Value> Iterable<Value>.toLookup( selector: (Value)->Key)
        = mutableMapOf<Key,MutableList<Value>> ()
        .also{map -> forEach { map.append(selector(it),it) }}