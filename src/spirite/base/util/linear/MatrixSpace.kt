package spirite.base.util.linear

import com.hackoeur.jglm.Mat
import java.util.*

/**
 * A MatrixSpace is a collection of Spaces and Matrices defining how to convert from one Space to another.  A connected
 * graph of transformations is given and all other transformations are calculated and cached as requested.
 */
class MatrixSpace<T : Enum<T>>(
        private val eclass: Class<T>,
        map: Map<Pair<T,T>, MatTrans>
)
{
    private val map = mutableMapOf<Pair<T,T>,MatTrans>()

    init {
        this.map.putAll(map)
    }

    fun convertSpace( from: T, to: T):MatTrans {
        if( from == to)
            return MatTrans()

        var key = Pair(from, to)
        if( map.containsKey( key))
            return map[key]!!
        var inverseKey = Pair(to,from)
        if( map.containsKey(inverseKey)) {
            val inv = map[inverseKey]!!.createInverse()
            map[key] = inv
            return inv
        }

        // Bredth-first search for a link
        val remaining = eclass.enumConstants.filter { it != from }
        val recurseQueue = LinkedList<MapNavigationState>()
        recurseQueue.add(
                MapNavigationState(from,remaining.toMutableList())
        )

        while( recurseQueue.isNotEmpty()) {
            val entry = recurseQueue.removeFirst()!!
            for( iTo in entry.remaining ) {
                key = Pair(entry.current, iTo)
                if( map.containsKey(key)) {
                    val trans = MatTrans(entry.transform)
                    trans.preConcatenate(map[key]!!)
                    if( iTo == to) {
                        map.put(Pair(from,to), trans)
                        return trans
                    }
                    recurseQueue.add(MapNavigationState(
                            iTo,
                            remaining.filter { it != iTo }.toMutableList(),
                            trans))
                }

                inverseKey = Pair(iTo, entry.current)
                if( map.containsKey(inverseKey)) {
                    val trans = MatTrans(entry.transform)
                    trans.preConcatenate(map[inverseKey]!!.createInverse())
                    if( iTo == to) {
                        map.put(Pair(from,to), trans)
                        return trans
                    }
                    recurseQueue.add(MapNavigationState(
                            iTo,
                            remaining.filter { it != iTo }.toMutableList(),
                            trans))
                }
            }
        }

        throw Exception("Matrix Space Grid is not connected.")
    }

    private inner class MapNavigationState(
            val current : T,
            val remaining : MutableList<T>,
            val transform : MatTrans = MatTrans()
    ){}
}