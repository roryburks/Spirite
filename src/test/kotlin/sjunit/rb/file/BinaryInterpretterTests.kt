package sjunit.rb.file

import org.junit.jupiter.api.Test
import rb.file.BigEndian
import rb.file.ByteInter
import rb.vectrix.mathUtil.i
import kotlin.test.assertEquals

object BinaryInterpreterTests {
    @Test fun byte() {
        val cases = byteArrayOf(0, 12, 111, 254.toByte())

        cases.forEach {
            assertEquals( it, ByteInter.interpret(ByteInter.convert(it)))
        }
    }

    @Test fun int() {
        val cvt = BigEndian.IntInter
        for (i in Int.MIN_VALUE until Int.MAX_VALUE) {
            assertEquals(i, cvt.interpret(cvt.convert(i)))
        }
    }

    @Test fun intArray() {
        val intArray = intArrayOf(12,-1234,1235,999,1124,12)
        val cvt = BigEndian.IntArrayInter(intArray.size)
        val doubleConverted = cvt.interpret(cvt.convert(intArray))
        assert(intArray.zip(doubleConverted){ a, b -> a == b }
            .all { it })
        assertEquals(intArray.size, doubleConverted.size)

    }

    @Test fun float() {
        val cases = floatArrayOf(0f, -0.0001f, 1000.12345f, 1432.14f, 12341f, 1.241f, -9.123465f)
        val cvt = BigEndian.FloatInter
        cases.forEach { f ->
            assertEquals(f, cvt.interpret(cvt.convert(f)))
        }
    }

    @Test fun floatArray() {
        val floatArray = floatArrayOf(0f, -0.0001f, 1000.12345f, 1432.14f, 12341f, 1.241f, -9.123465f)
        val cvt = BigEndian.FloatArrayInter(floatArray.size)
        val doubleConverted = cvt.interpret(cvt.convert(floatArray))
        assert(floatArray.zip(doubleConverted){ a, b -> a == b }
            .all { it })
        assertEquals(floatArray.size, doubleConverted.size)
    }

    @Test fun uByteInter() {
        val cvt = BigEndian.UByteInter
        for (i in (0..255)) {
            assertEquals(i, cvt.interpret(cvt.convert(i)))
        }
    }

    @Test fun shortInter() {
        val cvt = BigEndian.ShortInter
        for (i in Short.MIN_VALUE..Short.MAX_VALUE) {
            assertEquals(i, cvt.interpret(cvt.convert(i.toShort())).i)
        }
    }

    @Test fun uShortInter(){
        val cvt = BigEndian.UShortInter
        for(i in 0..65535) {
            assertEquals(i, cvt.interpret(cvt.convert(i)))
        }
    }
}