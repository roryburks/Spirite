package old.hydra


import kotlinx.coroutines.runBlocking
import rb.hydra.miniTiamatGrind
import rb.vectrix.mathUtil.d
import kotlin.test.assertEquals
import org.junit.Test as test

class MiniTiamatTests {
    @test fun Minimizes_Basic(){
        val things = listOf(1, 2, 3, 4, 6, -4)

        val min =  runBlocking {things.asSequence().miniTiamatGrind { it.d }}

        assertEquals(-4, min!!.second)
    }

    @test fun Minimizes_LessThan(){
        val things = listOf(1, -2, 3)

        val min =  runBlocking {things.asSequence().miniTiamatGrind { it.d }}

        assertEquals(-2, min!!.second)
    }
}