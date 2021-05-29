package rb.extendo.dataStructures

/***
 * A MutableSparseArray has the following basic behavior:
 * You can add/get from any index.  If no entity exists for that index, it returns null.
 * You can iterate over it.
 */
class MutableSparseArray<T> : Iterable<T> {
    constructor() {}
    constructor(toFillFrom: List<Pair<Int,T>>){
        val sorted = toFillFrom.sortedBy { it.first }
        for (pair in sorted) {
            _backingList.add(pair)
            _indexMap[pair.first] = _backingList.lastIndex
        }
    }

    fun get(i: Int) : T? = _indexMap[i]?.run { _backingList[this]?.second }

    fun set(i: Int, t:T) {
        when( val currentItem = _indexMap[i]){
            null -> {
                val nextIndex = _backingList.indexOfFirst { it.first > i } // Note: Could do as BST for more optimal
                when( nextIndex) {
                    -1 -> {
                        _backingList.add(Pair(i,t))
                        _indexMap[i] = _backingList.lastIndex
                    }
                    else -> {
                        _backingList.lastIndex.downTo(nextIndex)
                            .forEach {
                                if( it == _backingList.lastIndex)
                                    _backingList.add(_backingList[it])
                                else
                                    _backingList[it+1] = _backingList[it]
                                _indexMap[_backingList[it].first] = it+1
                            }
                        _backingList[nextIndex] = Pair(i,t)
                        _indexMap[i] = nextIndex
                    }
                }

            }
            else -> {
                _backingList[currentItem] = Pair(i,t)
            }
        }
    }
    fun remove( i:Int) : T? { TODO()}

    fun values() = _backingList.toList()

    override fun iterator(): Iterator<T> {
        return _backingList
            .map{ it.second}
            .iterator()
    }

    private val _backingList = mutableListOf<Pair<Int,T>>()
    private val _indexMap = mutableMapOf<Int, Int>()
}