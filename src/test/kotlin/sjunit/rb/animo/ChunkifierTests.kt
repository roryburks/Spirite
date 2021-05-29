//package sjunit.rb.animo
//
//import org.junit.jupiter.api.Test
//import rb.animo.animation.AafAnimStructure
//import rb.animo.animation.AafChunk
//import rb.animo.animation.AafFrame
//import rb.animo.animation.AafStructure
//import rb.extendo.dataStructures.SinglyList
//import rb.vectrix.shapes.RectI
//import kotlin.test.assertEquals
//
//object ChunkifierTests {
//    @Test fun chunkifies() {
//        val r1 = RectI(10,10,25,25)
//        val r1_dup = RectI(10,10,25,25)
//        val r2 = RectI(10,10,5,25)
//        val r3 = RectI(10,1,25,25)
//
//        val rects = listOf(r1,r1_dup, r2, r3)
//        val x = AafStructure(
//                SinglyList(AafAnimStructure(
//                        "name",
//                        SinglyList(AafFrame(rects.map { AafChunk(
//                                it, 0, 0, 0, ' '
//                        ) }))
//                ))
//        )
//
//        val chunked = Chunkifier.aggregateLikeChunks(x)
//
//        assertEquals(3, chunked.count())
//    }
//}