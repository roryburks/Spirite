package rb.vectrix.compaction

import rb.vectrix.VectrixMathLayer
import kotlin.math.max

class ReverseFloatCompactor( chunkSize: Int = 1024) {
    private val chunkSize : Int = max(1, chunkSize)
    private val data = mutableListOf<FloatArray>()
    var size = 0 ; private set

    fun add( i: Float) {
        if( size != Int.MAX_VALUE) {
            if( size % chunkSize == 0) {
                data.add(FloatArray(chunkSize))
            }
            data[size/chunkSize][chunkSize-(size % chunkSize) - 1] = i
            ++size
        }
    }

    operator fun get(n: Int) = data[n / chunkSize][chunkSize-(n%chunkSize)-1]

    val chunkCount get() = data.size
    fun getChunk( i: Int) = data[i]
    fun getChunkSize( i: Int) = if( i < data.size - 1) chunkSize else size % chunkSize

    fun toArray() = FloatArray(size).also { insertIntoArray(it, 0) }

    fun insertIntoArray( array: FloatArray, start:Int) {
        if( size == 0) return

        val leadingOffset = getChunkSize(data.size-1)
        VectrixMathLayer.arraycopy(data[data.size-1], chunkSize - leadingOffset, array, start, leadingOffset)

        ( 1 until data.size).forEach {i ->
            val index = data.size - i - 1
            VectrixMathLayer.arraycopy(data[index], 0, array, start + chunkSize*(i-1)+leadingOffset, chunkSize)
        }
    }
}