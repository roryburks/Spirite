package sjunit.util


import spirite.base.util.linear.MatTrans
import spirite.base.util.linear.MatrixSpace
import spirite.base.util.linear.Vec2
import org.junit.Test as test

class MatrixSpaceTests {

    @test fun TestConnectedness() {
        val map = mapOf(
                Pair("S1","S2") to MatTrans.TranslationMatrix(50f,50f),
                Pair("S2", "S3") to MatTrans.TranslationMatrix(25f, 25f),
                Pair("S1", "S4") to MatTrans.TranslationMatrix(100f, 100f)
        )

        val space = MatrixSpace(
                map
        )
        var verifyMap = mutableMapOf(
                Pair("S1","S2") to Vec2(50f,50f),
                Pair("S1","S3") to Vec2(75f,75f),
                Pair("S1","S4") to Vec2(100f,100f),
                Pair("S2","S1") to Vec2(-50f,-50f),
                Pair("S2","S3") to Vec2(25f,25f),
                Pair("S2","S4") to Vec2(50f,50f),
                Pair("S3","S1") to Vec2(-75f,-75f),
                Pair("S3","S2") to Vec2(-25f,-25f),
                Pair("S3","S4") to Vec2(25f,25f),
                Pair("S4","S1") to Vec2(-100f,-100f),
                Pair("S4","S2") to Vec2(-50f,-50f),
                Pair("S4","S3") to Vec2(-25f,-25f)
        )

        for( entry in verifyMap) {
            val test = space.convertSpace(entry.key.first,entry.key.second).transform(Vec2(0f,0f))
            assert(test.x == entry.value.x && test.y == entry.value.y)
        }
    }
}