package sjunit.rb.extendo.extensions

import org.junit.jupiter.api.Test
import rb.extendo.extensions.stride
import kotlin.test.assertEquals

class StrideTests {
    @Test fun strideTest_1(){
        val list = listOf(1)

        val stridelist = list.stride(10)

        assertEquals(1, stridelist.count())
        assertEquals(1, stridelist[0])
    }

    @Test fun strideTest_0(){
        val list = listOf<Int>()

        val stridelist = list.stride(10)

        assertEquals(0, stridelist.count())
    }

    @Test fun strideTest_10(){
        val list = listOf<Int>(0,1,2,3,4,5,6,7,8,9)

        val stridelist = list.stride(10)

        assertEquals(1, stridelist.count())
        assertEquals(0, stridelist[0])
    }

    @Test fun strideTest_11(){
        val list = listOf<Int>(0,1,2,3,4,5,6,7,8,9,10)

        val stridelist = list.stride(10)

        assertEquals(2, stridelist.count())
        assertEquals(0, stridelist[0])
        assertEquals(10, stridelist[1])
    }

    @Test fun strideTest_21(){
        val list = (0..20).toList()

        val stridelist = list.stride(10)

        assertEquals(3, stridelist.count())
        assertEquals(0, stridelist[0])
        assertEquals(10, stridelist[1])
        assertEquals(20, stridelist[2])
    }
}