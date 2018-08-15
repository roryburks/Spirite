package spirite.base.util.groupExtensions


fun <Key,Value> MutableMap<Key,MutableList<Value>>.append( key: Key, value: Value) {
    (this[key] ?: mutableListOf<Value>().also{this[key] = it}).add(value)
}

fun <Key,Value> Map<Key,List<Value>>.lookup( key: Key) : List<Value> = this[key] ?: emptyList()