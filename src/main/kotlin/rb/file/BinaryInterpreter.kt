package rb.file

import rb.vectrix.mathUtil.i
import rb.vectrix.mathUtil.ui
import kotlin.experimental.or

interface IBinaryInterpreter<T> {
    val len: Int
    fun interpret(byteArray: ByteArray) : T
}

object LittleEndian {
    object IntInter : IBinaryInterpreter<Int> {
        override val len: Int get() = 4
        override fun interpret(byteArray: ByteArray): Int {
            var a1 =  byteArray[0].ui
            var a2 =  byteArray[1].ui
            var a3 =  byteArray[2].ui
            var a4 =  byteArray[3].ui
            return byteArray[0].ui or
                    (byteArray[1].ui shl 8) or
                    (byteArray[2].ui shl 16) or
                    (byteArray[3].ui shl 24)
        }
    }
}