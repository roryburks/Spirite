package rb.file

import rb.vectrix.mathUtil.b
import rb.vectrix.mathUtil.i
import rb.vectrix.mathUtil.ui
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface IBinaryInterpreter<T> {
    val len: Int
    fun interpret(byteArray: ByteArray) : T
    fun convert( t: T) : ByteArray
}

object ByteInter : IBinaryInterpreter<Byte> {
    override val len: Int get() = 1
    override fun interpret(byteArray: ByteArray) = byteArray[0]
    override fun convert(t: Byte): ByteArray = byteArrayOf(t)
}

class ByteArrayInter(val n : Int) : IBinaryInterpreter<ByteArray> {
    override val len: Int get() =  n
    override fun interpret(byteArray: ByteArray) = byteArray
    override fun convert(t: ByteArray): ByteArray = t
}

object BigEndian {
    // God, Kotlin's bitwise capabilities are /*inexcusable*/ really bad but at least better
    object IntInter : IBinaryInterpreter<Int> {
        override val len: Int get() = 4
        override fun interpret(byteArray: ByteArray): Int {
            return byteArray[3].ui or
                    (byteArray[2].ui shl 8) or
                    (byteArray[1].ui shl 16) or
                    (byteArray[0].ui shl 24)
        }

        override fun convert(t: Int): ByteArray {
            return byteArrayOf(
                (t shr 24).toByte(),
                (t shr 16).toByte(),
                (t shr 8).toByte(),
                t.toByte() )
        }
    }

    class IntArrayInter(val n: Int) : IBinaryInterpreter<IntArray> {
        override val len: Int get() = n*4

        override fun interpret(byteArray: ByteArray): IntArray {
            return IntArray(n) { it ->
                byteArray[3 + it*4].ui or
                        (byteArray[2 + it*4].ui shl 8) or
                        (byteArray[1 + it*4].ui shl 16) or
                        (byteArray[0 + it*4].ui shl 24)
            }
        }

        override fun convert(t: IntArray): ByteArray {
            val ba = ByteArray(n*4)
            for (index in (0 until n)) {
                val i = t[index]
                ba[0 + index*4] = (i shr 24).toByte()
                ba[1 + index*4] = (i shr 16).toByte()
                ba[2 + index*4] = (i shr 8).toByte()
                ba[3 + index*4] = i.toByte()
            }
            return ba
        }

    }

    object FloatInter : IBinaryInterpreter<Float> {
        override val len: Int get() = 4

        override fun interpret(byteArray: ByteArray): Float {
            return Float.fromBits(IntInter.interpret(byteArray))
//            val w = ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN)
//            return w.float
        }

        override fun convert(t: Float): ByteArray {
            return IntInter.convert(t.toRawBits())
//            val bb = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
//            bb.putFloat(t)
//            return bb.array()
        }
    }
    class FloatArrayInter(val n: Int) : IBinaryInterpreter<FloatArray> {
        override val len: Int get() = n*4
        override fun interpret(byteArray: ByteArray): FloatArray {
            val ia = IntArrayInter(n).interpret(byteArray)
            return FloatArray(n){ Float.fromBits(ia[it])}

//            val w = ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN)
//            val fa = FloatArray(n) { w.float}
//            return fa
        }

        override fun convert(t: FloatArray): ByteArray {
            val ia = IntArray(n){ t[it].toRawBits()}
            return IntArrayInter(n).convert(ia)
        }
    }

    object UByteInter : IBinaryInterpreter<Int> {
        override val len: Int get() = 1
        override fun interpret(byteArray: ByteArray) =  byteArray[0].ui
        override fun convert(t: Int): ByteArray { return ByteArray(1) {t.toByte()}}
    }

    object ShortInter : IBinaryInterpreter<Short> {
        override val len: Int get() = 2
        override fun interpret(byteArray: ByteArray): Short {
            return (byteArray[1].ui or
                    (byteArray[0].ui shl 8)).toShort()
        }

        override fun convert(t: Short): ByteArray {
            return byteArrayOf(
                (t.i shr 8).b,
                t.toByte() )
        }

    }

    object UShortInter : IBinaryInterpreter<Int> {
        override val len: Int get() = 2
        override fun interpret(byteArray: ByteArray): Int {
            return byteArray[1].ui or
                    (byteArray[0].ui shl 8)
        }

        override fun convert(t: Int): ByteArray {
            return byteArrayOf(
                (t shr 8).b,
                t.b )
        }
    }
}