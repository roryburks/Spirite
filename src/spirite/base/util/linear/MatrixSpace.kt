package spirite.base.util.linear

import spirite.base.util.linear.Transform.Companion
import java.util.*

/**
 * A MatrixSpace is a collection of Spaces and Matrices defining how to convert from one Space to another.  A (preferably)
 * connected graph of transformations is given and all other transformations are calculated and cached as requested.
 */
class MatrixSpace(
        map: Map<Pair<String,String>, Transform>
)
{
    private val map = mutableMapOf<Pair<String,String>,Transform>()
    private val keys : List<String>

    init {
        this.map.putAll(map)
        keys = (map.keys.map { it.first } union map.keys.map{it.second}).distinct()
    }

    fun extend(moreEntries: Map<Pair<String,String>, Transform>) :MatrixSpace {
        val newMap = mutableMapOf<Pair<String,String>,Transform>()
        newMap.putAll(map)
        newMap.putAll(moreEntries)
        return MatrixSpace( newMap)
    }

    fun convertSpace( from: String, to: String):Transform {
        // Note: could cache all the transforms from A to B during the attempt to find a link
        if( from == to)
            return Transform.IdentityMatrix

        var key = Pair(from, to)
        if( map.containsKey( key))
            return map[key]!!
        var inverseKey = Pair(to,from)
        if( map.containsKey(inverseKey)) {
            val inv = map[inverseKey]!!.invert()
            map[key] = inv
            return inv
        }

        // Bredth-first search for a link
        val remaining = keys.filter { it != from }
        val recurseQueue = LinkedList<MapNavigationState>()
        recurseQueue.add(
                MapNavigationState(from,remaining.toMutableList())
        )

        while( recurseQueue.isNotEmpty()) {
            val entry = recurseQueue.removeFirst()!!
            for( iTo in entry.remaining ) {
                key = Pair(entry.current, iTo)
                if( map.containsKey(key)) {
                    val trans = map[key]!! * entry.transform
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
                    val trans = map[inverseKey]!!.invert() * entry.transform
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

        throw Exception("Requested nodes are not connected.")
    }

    private inner class MapNavigationState(
            val current : String,
            val remaining : MutableList<String>,
            val transform : Transform = Transform.IdentityMatrix
    ){}
}