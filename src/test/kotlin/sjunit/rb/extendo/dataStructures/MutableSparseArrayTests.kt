package sjunit.rb.extendo.dataStructures

import org.junit.jupiter.api.Test
import rb.extendo.dataStructures.MutableSparseArray
import rb.extendo.extensions.fillToList
import kotlin.test.assertEquals

object MutableSparseArrayTests {
    @Test fun testInserts(){
        val msa = MutableSparseArray<Float>()

        // 1
        val array1 =  msa.iterator().fillToList()
        assert(array1.isEmpty())

        // 2
        msa.set(100042, 0.5f)
        val array2 = msa.iterator().fillToList()
        assert( array2.size == 1)
        assert( array2[0] == 0.5f)

        // 3
        msa.set(-100042, 1.25f)
        val array3 = msa.iterator().fillToList()
        assert( array3.size == 2)
        assert( array3[0] == 01.25f)
        assert( array3[1] == 0.5f)

        // 4
        msa.set(13, 14.0f)
        val list4 = msa.iterator().fillToList()
        assert(list4.size == 3)
        assert( list4[0] == 01.25f)
        assert( list4[1] == 14.0f)
        assert( list4[2] == 0.5f)

        assertEquals(0.5f, msa.get(100042))
        assertEquals(1.25f, msa.get(-100042))
        assertEquals(14f, msa.get(13))
    }
}