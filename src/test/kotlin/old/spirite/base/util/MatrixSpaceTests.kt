//package old.spirite.base.util
//
//
//import rb.vectrix.linear.MatrixSpace
//import rb.vectrix.linear.Vec2f
//import org.junit.jupiter.api.Test as test
//
//class MatrixSpaceTests {
//
//    @test fun TestConnectedness() {
//        val map = mapOf(
//                Pair("S1","S2") to ImmutableTransformF.Translation(50f,50f),
//                Pair("S2", "S3") to ImmutableTransformF.Translation(25f, 25f),
//                Pair("S1", "S4") to ImmutableTransformF.Translation(100f, 100f)
//        )
//
//        val space = MatrixSpace(map)
//        val verifyMap = mutableMapOf(
//                Pair("S1","S2") to Vec2f(50f,50f),
//                Pair("S1","S3") to Vec2f(75f,75f),
//                Pair("S1","S4") to Vec2f(100f,100f),
//                Pair("S2","S1") to Vec2f(-50f,-50f),
//                Pair("S2","S3") to Vec2f(25f,25f),
//                Pair("S2","S4") to Vec2f(50f,50f),
//                Pair("S3","S1") to Vec2f(-75f,-75f),
//                Pair("S3","S2") to Vec2f(-25f,-25f),
//                Pair("S3","S4") to Vec2f(25f,25f),
//                Pair("S4","S1") to Vec2f(-100f,-100f),
//                Pair("S4","S2") to Vec2f(-50f,-50f),
//                Pair("S4","S3") to Vec2f(-25f,-25f)
//        )
//
//        for( entry in verifyMap) {
//            val test = space.convertSpace(entry.key.first,entry.key.second).apply(Vec2f(0f,0f))
//            assert(test.xf == entry.value.xf && test.yf == entry.value.yf)
//        }
//    }
//}