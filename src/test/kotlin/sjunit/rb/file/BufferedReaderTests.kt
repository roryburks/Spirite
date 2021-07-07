package sjunit.rb.file

import org.junit.jupiter.api.Test
import rb.file.BufferedReadStream
import rb.file.ByteArrayReadStream
import rb.vectrix.mathUtil.b
import rbJvm.vectrix.SetupVectrixForJvm
import kotlin.test.assertEquals

object BufferedReaderTests {
    @Test fun testReadInt() {
        SetupVectrixForJvm()
        val bytes = convertToByteBigEndian ((0..1000).toList().toIntArray())
        val stream = ByteArrayReadStream(bytes)
        val bufferedReader = BufferedReadStream(stream, 55) // intentionally nonsensical size

        for (i in 0..1000){
            if( i == 1000) {
                val y = 2
            }
            val x = bufferedReader.readInt()
            val j = x+i
             assertEquals(i, x)

        }
    }

    @Test fun testReadInt_Negative() {
        SetupVectrixForJvm()
        val bytes = convertToByteBigEndian ((0..1000).toList().map { -it }.toIntArray())
        val stream = ByteArrayReadStream(bytes)
        val bufferedReader = BufferedReadStream(stream, 5) // intentionally nonsensical size

        for (i in 0..1000){
            if( i == 1000) {
                val y = 2
            }
            val x = bufferedReader.readInt()
            val j = x+i
            assertEquals(-i, x)

        }
    }

    private fun convertToByteBigEndian(intArray: IntArray) : ByteArray {
        val byteArray = ByteArray(intArray.size * 4)
        intArray.forEachIndexed { index, i ->
            byteArray[index*4+0] = ((i shr 24) and 255).b
            byteArray[index*4+1] = ((i shr 16) and 255).b
            byteArray[index*4+2] = ((i shr 8) and 255).b
            byteArray[index*4+3] = (i and 255).b
        }
        return byteArray
    }
}