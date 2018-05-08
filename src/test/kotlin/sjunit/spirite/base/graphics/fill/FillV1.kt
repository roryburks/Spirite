package sjunit.spirite.base.graphics.fill

import spirite.base.graphics.fill.V0FillArrayAlgorithm
import spirite.base.graphics.fill.V1FillArrayAlgorithm
import spirite.base.graphics.gl.GLImage
import spirite.base.util.Colors
import spirite.hybrid.EngineLaunchpoint
import kotlin.experimental.and
import kotlin.system.measureTimeMillis
import org.junit.Test as test

class FillV1Tests {
    @test fun TestFill9() {
        TestFill(9)
    }
    @test fun TestFill18() {
        TestFill(18)
    }
    @test fun TestFill400() {
        TestFill(400)
    }
    @test fun TestFill2000() {
            TestFill(2000)
    }
    fun TestFill( r: Int) {

        val image = GLImage(r,r,EngineLaunchpoint.gle, true)
        val w = image.width
        val h = image.height

        image.graphics.drawLine( 0, 0, w,h)

        lateinit var iii: IntArray
//        println(measureTimeMillis {
//            iii = //GLFillV1.fill(image, 3, 2, Colors.BLACK) ?: throw Exception("bad")
//
//                    V1FillArrayAlgorithm.fill(
//                            image.toIntArray() ?: throw Exception(),
//                            image.width, image.height, 3, 2, Colors.BLACK.argb32) ?: throw Exception("bad")
//        })

        println(measureTimeMillis {
            iii = //GLFillV1.fill(image, 3, 2, Colors.BLACK) ?: throw Exception("bad")
                    V0FillArrayAlgorithm.fill(
                            image.toIntArray() ?: throw Exception(),
                            image.width, image.height, 3, 2, Colors.BLACK.argb32) ?: throw Exception("bad")
        })

        val faW = (w-1) / 8 + 1
        (0 until w).forEach { x ->
            (0 until h).forEach { y ->
                if( iii[x / 8 + y / 4 * faW] and (1 shl ((x % 8) + (y % 4)*8)) != 0) {
                    assert( y < x)
                }
                else assert( y >= x)
            }
        }
        println("really")
    }

}