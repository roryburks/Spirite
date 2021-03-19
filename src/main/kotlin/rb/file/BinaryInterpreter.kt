package rb.file

import rb.vectrix.mathUtil.ui
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface IBinaryInterpreter<T> {
    val len: Int
    fun interpret(byteArray: ByteArray) : T
}

object ByteInter : IBinaryInterpreter<Byte> {
    override val len: Int get() = 1
    override fun interpret(byteArray: ByteArray) = byteArray[0]
}

class ByteArrayInter(val n : Int) : IBinaryInterpreter<ByteArray> {
    override val len: Int get() =  n
    override fun interpret(byteArray: ByteArray) = byteArray
}

object LittleEndian {
    // God, Kotlin's bitwise capabilities are inexcusable
    object IntInter : IBinaryInterpreter<Int> {
        override val len: Int get() = 4
        override fun interpret(byteArray: ByteArray): Int {
            return byteArray[3].ui or
                    (byteArray[2].ui shl 8) or
                    (byteArray[1].ui shl 16) or
                    (byteArray[0].ui shl 24)
        }
    }

    object FloatInter : IBinaryInterpreter<Float> {
        override val len: Int get() = 4

        override fun interpret(byteArray: ByteArray): Float {
            val w = ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN)
            return w.float
        }
    }
    class FloatArrayInter(val n: Int) : IBinaryInterpreter<FloatArray> {
        override val len: Int get() = n*4
        override fun interpret(byteArray: ByteArray): FloatArray {
            val w = ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN)
            val fa = FloatArray(n) { w.float}

            return fa
        }
    }

    object UByteInter : IBinaryInterpreter<Int> {
        override val len: Int get() = 1
        override fun interpret(byteArray: ByteArray) =  byteArray[0].ui
    }

    object ShortInter : IBinaryInterpreter<Short> {
        override val len: Int get() = 2
        override fun interpret(byteArray: ByteArray): Short {
            return (byteArray[1].ui or
                    (byteArray[0].ui shl 8)).toShort()
        }

    }

    object UShortInter : IBinaryInterpreter<Int> {
        override val len: Int get() = 2
        override fun interpret(byteArray: ByteArray): Int {
            return byteArray[1].ui or
                    (byteArray[0].ui shl 8)
        }
    }
}