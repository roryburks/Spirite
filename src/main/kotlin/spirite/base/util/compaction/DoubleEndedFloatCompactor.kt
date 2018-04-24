package spirite.base.util.compaction

import kotlin.math.max

class DoubleEndedFloatCompactor( chunkSize : Int = 1024) {
    private val chunkSize = max(1,chunkSize)
    private var tail = chunkSize/2-1
    private var head = chunkSize/2
    private val data = mutableListOf<FloatArray>(FloatArray(this.chunkSize))

    fun addHead( i: Float) {
        if( head == chunkSize) {
            data.add(FloatArray(chunkSize))
            head = 0
        }
        data.last()[head] = i
        ++head
    }

    fun addTail( i: Float) {
        if( tail == -1) {
            data.add(0, FloatArray(chunkSize))
            tail = chunkSize-1
        }
        data.first()[tail] = i
        --tail
    }

    val size : Int get() = if( data.size == 1) head - tail - 1
        else (data.size - 2) * chunkSize + (head) + (chunkSize-tail)

    val chunkCount : Int get() = data.size

    fun toArray() = FloatArray(size).also { insertIntoArray(it, 0) }

    fun insertIntoArray( array: FloatArray, start:Int) {
        if( data.size == 1)
            System.arraycopy(data.first(), tail+1, array, start, head-tail-1)
        else {
            val it = data.iterator()
            var caret = start
            // First
            System.arraycopy(it.next(), tail+1, array, caret, chunkSize-tail-1)
            caret += chunkSize - tail - 1
            while( it.hasNext()) {
                val chunk = it.next()
                when( it.hasNext()) {
                    true -> {
                        // Middle
                        System.arraycopy(chunk, 0, array, caret, chunkSize)
                        caret += chunkSize
                    }
                    false -> System.arraycopy(chunk, 0, array, caret, head)
                }
            }
        }
    }
}