package sjunit.util


import spirite.base.util.linear.MatTrans
import spirite.base.util.linear.MatrixSpace
import spirite.base.util.linear.Vec2
import org.junit.Test as test

class MatrixSpaceTests {
    private enum class TestSpaces {
        S1, S2, S3, S4
    }

    @test fun TestConnectedness() {
        val map = mutableMapOf(
                Pair(TestSpaces.S1,TestSpaces.S2) to MatTrans.TranslationMatrix(50f,50f),
                Pair(TestSpaces.S2, TestSpaces.S3) to MatTrans.TranslationMatrix(25f, 25f),
                Pair(TestSpaces.S1, TestSpaces.S4) to MatTrans.TranslationMatrix(100f, 100f)
        )

        val space = MatrixSpace<TestSpaces>(
                TestSpaces::class.java,
                map
        )
        var verifyMap = mutableMapOf(
                Pair(TestSpaces.S1,TestSpaces.S2) to Vec2(50f,50f),
                Pair(TestSpaces.S1,TestSpaces.S3) to Vec2(75f,75f),
                Pair(TestSpaces.S1,TestSpaces.S4) to Vec2(100f,100f),
                Pair(TestSpaces.S2,TestSpaces.S1) to Vec2(-50f,-50f),
                Pair(TestSpaces.S2,TestSpaces.S3) to Vec2(25f,25f),
                Pair(TestSpaces.S2,TestSpaces.S4) to Vec2(50f,50f),
                Pair(TestSpaces.S3,TestSpaces.S1) to Vec2(-75f,-75f),
                Pair(TestSpaces.S3,TestSpaces.S2) to Vec2(-25f,-25f),
                Pair(TestSpaces.S3,TestSpaces.S4) to Vec2(25f,25f),
                Pair(TestSpaces.S4,TestSpaces.S1) to Vec2(-100f,-100f),
                Pair(TestSpaces.S4,TestSpaces.S2) to Vec2(-50f,-50f),
                Pair(TestSpaces.S4,TestSpaces.S3) to Vec2(-25f,-25f)
        )

        for( entry in verifyMap) {
            val test = space.convertSpace(entry.key.first,entry.key.second).transform(Vec2(0f,0f))
            assert(test.x == entry.value.x && test.y == entry.value.y)
        }
    }
}