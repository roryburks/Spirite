package sjunit.rb.alexandria.io

import org.junit.jupiter.api.Test
import rb.alexandria.io.BufferedWriteStream
import rb.alexandria.io.ByteListWriteStream
import rb.vectrix.mathUtil.b
import rbJvm.vectrix.SetupVectrixForJvm
import kotlin.test.assertEquals

class BufferedWriteStreamTests {
    init {
        SetupVectrixForJvm()
    }

    @Test fun writes_unbatched() {
        val baseStream = ByteListWriteStream()
        val stream = BufferedWriteStream(baseStream, 10)

        // Act
        val toWrite = ByteArray(8) {it.b}
        stream.write(toWrite)
        stream.finish()

        // Assert
        assertEquals(8, baseStream.list.size)
        (0..7).forEach { assertEquals(baseStream.list[it], it.b) }
    }

    @Test fun writes_batched(){
        val baseStream = ByteListWriteStream()
        val stream = BufferedWriteStream(baseStream, 10)

        // Act
        (0..7).forEach { x ->
            val toWrite = ByteArray(8){(x*10 + it).b}
            stream.write(toWrite)
        }
        stream.finish()

        // Assert
        assertEquals(8*8, baseStream.list.size)
        (0..7).forEach { x ->
            (0..7).forEach { y -> assertEquals((x*10+y).b, baseStream.list[x*8+y]) }
        }
    }

    @Test fun writes_smallBuffers(){
        // This test is written to hit the chunk of code for when a Buffer already exists and the write isn't big enough
        // to cause a buffer clear.  So 4 + 4 = 8 < 12, then 4 again
        val baseStream = ByteListWriteStream()
        val stream = BufferedWriteStream(baseStream, 10)

        // Act
        (0..3).forEach { x ->
            val toWrite = ByteArray(4){(x*10 + it).b}
            stream.write(toWrite)
        }
        stream.finish()

        // Assert
        assertEquals(4*4, baseStream.list.size)
        (0..3).forEach { x ->
            (0..3).forEach { y -> assertEquals((x*10+y).b, baseStream.list[x*4+y]) }
        }
    }

    @Test fun writes_largeBuffer_New() {
        // In this case, you're writing what is far larger than the buffer size
        val baseStream = ByteListWriteStream()
        val stream = BufferedWriteStream(baseStream, 10)

        // Act
        val toWrite = ByteArray(50) {it.b}
        stream.write(toWrite)
        stream.finish()

        // Assert
        assertEquals(50, baseStream.list.size)
        (0..49).forEach { assertEquals(it.b, baseStream.list[it]) }
    }

    @Test fun writes_largeBuffer_FromMiddle() {
        // As Above, but you're writing from a buffer which is mid-write
        val baseStream = ByteListWriteStream()
        val stream = BufferedWriteStream(baseStream, 10)

        // Act
        val toWrite1 = ByteArray(5) {it.b}
        stream.write(toWrite1)
        val toWrite2 = ByteArray(50) {it.b}
        stream.write(toWrite2)
        stream.finish()

        // Assert
        assertEquals(55, baseStream.list.size)
        (0..4).forEach { assertEquals(it.b, baseStream.list[it]) }
        (0..49).forEach { assertEquals(it.b, baseStream.list[it+5]) }
    }

    @Test fun goto_midBatch() {
        // Basic idea: Batch to 15 by going 5, 5, 5, then seek to 3, then write 8
        // first pass: 012340123401234
        // result:     012012345671234
        val baseStream = ByteListWriteStream()
        val stream = BufferedWriteStream(baseStream, 10)

        // Act
        repeat(3) {stream.write(ByteArray(5){it.b})}
        assertEquals(15, stream.pointer)
        stream.goto(3)
        assertEquals(3, stream.pointer)
        stream.write(ByteArray(8){it.b})
        assertEquals(11, stream.pointer)
        stream.finish()

        // Assert
        assertEquals(15, baseStream.list.size)
        val x = byteArrayOf(0,1,2,0,1,2,3,4,5,6,7,1,2,3,4)
        (0..14).forEach { assertEquals(x[it], baseStream.list[it]) }
    }

}