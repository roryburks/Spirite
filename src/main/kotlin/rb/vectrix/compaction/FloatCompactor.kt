package rb.vectrix.compaction

import rb.vectrix.VectrixMathLayer
import kotlin.math.max

class FloatCompactor( chunkSize: Int = 1024) {
    private val chunkSize = max(1,chunkSize)
    private val data = mutableListOf<FloatArray>()
    var size = 0 ; private set
    val chunkCount: Int get() = data.size

    fun add( i: Float) {
        if( size != Int.MAX_VALUE) {
            if( size % chunkSize == 0)
                data.add(FloatArray(chunkSize))
            data[size / chunkSize] [size % chunkSize] = i
            ++size
        }
    }

    operator fun get( n: Int) = data[n / chunkSize][n%chunkSize]
    fun getChunk(i: Int) = data[i]
    fun getChunkSize(i : Int) = if( i < data.size-1) chunkSize else size % chunkSize

    fun toArray() : FloatArray = FloatArray(size).also { insertIntoArray(it, 0) }

    fun insertIntoArray( array: FloatArray, start: Int) {
        data.forEachIndexed { i, datum ->
            VectrixMathLayer.arraycopy(datum, 0, array, start + chunkSize*i, getChunkSize(i))
        }
    }

}