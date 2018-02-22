package sjunit.spirite.util


import spirite.base.util.StringUtil
import kotlin.test.assertEquals
import org.junit.Test as test

class StringUtilTests {

    @test fun CreatesDuplicateNames() {
        val list = listOf("a", "a_0", "a_1", "a_3")

        val result = StringUtil.getNonDuplicateName(list, "a")

        assertEquals( "a_2", result)
    }
    @test fun CreatesDuplicateNames2() {
        val list = listOf<String>()

        val result = StringUtil.getNonDuplicateName(list, "a")

        assertEquals( "a", result)
    }
    @test fun CreatesDuplicateNames3() {
        val list = listOf("a")

        val result = StringUtil.getNonDuplicateName(list, "a_1")

        assertEquals( "a_1", result)
    }
    @test fun CreatesDuplicateNames4() {
        val list = listOf("a_1", "a_2", "a_3")

        val result = StringUtil.getNonDuplicateName(list, "a_1")

        assertEquals( "a_4", result)
    }
}