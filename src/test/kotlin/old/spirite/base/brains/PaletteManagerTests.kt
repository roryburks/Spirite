//package old.spirite.base.brains
//
//import spirite.base.brains.palette.IPaletteManager.PaletteObserver
//import spirite.base.brains.palette.PaletteManager
//import kotlin.test.assertEquals
//import org.junit.Test as test
//
//class PaletteManagerTests {
//    val paletteManager = PaletteManager()
//
//    @test fun SetsAndGets() {
//        paletteManager.setActiveColor(0, 1)
//        paletteManager.setActiveColor(1, 2)
//        paletteManager.setActiveColor(2, 3)
//        paletteManager.setActiveColor(3, 4)
//
//        assertEquals(1, paletteManager.getActiveColor(0))
//        assertEquals(2, paletteManager.getActiveColor(1))
//        assertEquals(3, paletteManager.getActiveColor(2))
//        assertEquals(4, paletteManager.getActiveColor(3))
//    }
//
//    @test fun Cycles() {
//        paletteManager.setActiveColor(0, 1)
//        paletteManager.setActiveColor(1, 2)
//        paletteManager.setActiveColor(2, 3)
//        paletteManager.setActiveColor(3, 4)
//
//        paletteManager.cycleActiveColors(1)
//
//        assertEquals(2, paletteManager.getActiveColor(0))
//        assertEquals(3, paletteManager.getActiveColor(1))
//        assertEquals(4, paletteManager.getActiveColor(2))
//        assertEquals(1, paletteManager.getActiveColor(3))
//
//        paletteManager.cycleActiveColors(-7)
//
//        assertEquals(3, paletteManager.getActiveColor(0))
//        assertEquals(4, paletteManager.getActiveColor(1))
//        assertEquals(1, paletteManager.getActiveColor(2))
//        assertEquals(2, paletteManager.getActiveColor(3))
//    }
//
//    @test fun Triggers() {
//        // Arrange
//        var triggered = false
//        paletteManager.paletteObservable.addObserver( object : PaletteObserver {
//            override fun paletteChanged() {
//                triggered = true
//            }
//        })
//
//        val paletteSet = paletteManager.makePaletteSet()
//
//        val palette = paletteSet.addPalette("Palette", false)
//
//        assert(triggered)
//
//        triggered = false
//
//        palette.setPaletteColor(8, 777)
//
//        assert(triggered)
//    }
//}