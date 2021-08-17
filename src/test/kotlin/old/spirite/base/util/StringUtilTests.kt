package old.spirite.base.util


import org.junit.jupiter.api.Test
import spirite.core.util.StringUtil
import kotlin.test.assertEquals

class StringUtilTests {

    @Test fun CreatesDuplicateNames() {
        val list = listOf("a", "a_0", "a_1", "a_3")

        val result = StringUtil.getNonDuplicateName(list, "a")

        assertEquals( "a_2", result)
    }
    @Test fun CreatesDuplicateNames2() {
        val list = listOf<String>()

        val result = StringUtil.getNonDuplicateName(list, "a")

        assertEquals( "a", result)
    }
    @Test fun CreatesDuplicateNames3() {
        val list = listOf("a")

        val result = StringUtil.getNonDuplicateName(list, "a_1")

        assertEquals( "a_1", result)
    }
    @Test fun CreatesDuplicateNames4() {
        val list = listOf("a_1", "a_2", "a_3")

        val result = StringUtil.getNonDuplicateName(list, "a_1")

        assertEquals( "a_4", result)
    }
}