package old.spirite.base.graphics.fill

import org.junit.jupiter.api.Test
import rb.glow.Colors
import rb.glow.drawer
import rb.glow.gl.GLImage
import rb.vectrix.mathUtil.d
import spirite.sguiHybrid.EngineLaunchpoint
import spirite.specialRendering.fill.V0FillArrayAlgorithm
import spirite.specialRendering.fill.toIntArray
import kotlin.system.measureTimeMillis

class FillV1Tests {
    @Test fun TestFill9() {
        TestFill(9)
    }
    @Test fun TestFill18() {
        TestFill(18)
    }
    @Test fun TestFill400() {
        TestFill(400)
    }
    @Test fun TestFill2000() {
            TestFill(2000)
    }
    fun TestFill( r: Int) {

        val image = GLImage(r, r, EngineLaunchpoint.gle, true)
        val w = image.width
        val h = image.height

        image.graphics.drawer.drawLine( 0.0, 0.0, w.d,h.d)

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
                            image.graphics.toIntArray() ?: throw Exception(),
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