package sjunit.spirite.base.util.dataContainers


import spirite.base.util.dataContainers.MutableOneToManyMap
import kotlin.test.assertEquals
import org.junit.Test as test

class OneToManyMapTests {
    @test fun Inserts() {
        val map = MutableOneToManyMap< Int, String>()

        map.assosciate("a", 1)
        map.assosciate("b", 1)
        map.assosciate("c", 1)

        assertEquals( 1, map.getOne("a"))
        assertEquals( 1, map.getOne("b"))
        assertEquals( 1, map.getOne("c"))
        assertEquals( 3, map.getMany(1)!!.size)
    }

    @test fun Removes() {
        val map = MutableOneToManyMap< Int, String>()

        map.assosciate("a", 1)
        map.assosciate("b", 1)
        map.assosciate("c", 1)

        map.dissociate("b")


        assertEquals( 1, map.getOne("a"))
        assertEquals( null, map.getOne("b"))
        assertEquals( 1, map.getOne("c"))
        assertEquals( 2, map.getMany(1)!!.size)
    }

    @test fun RemovesAndDeletes() {
        val map = MutableOneToManyMap< Int, String>()

        map.assosciate("a", 1)
        map.assosciate("b", 1)

        map.dissociate("b")
        map.dissociate("a")


        assertEquals( null, map.getMany(1))
    }

    @test fun Reassosciates() {
        val map = MutableOneToManyMap< Int, String>()

        map.assosciate("a", 1)
        map.assosciate("b", 1)
        map.assosciate("c", 1)

        map.assosciate("b", 2)


        assertEquals( 1, map.getOne("a"))
        assertEquals( 2, map.getOne("b"))
        assertEquals( 1, map.getOne("c"))
        assertEquals( 2, map.getMany(1)!!.size)
        assertEquals( 1, map.getMany(2)!!.size)
    }
}