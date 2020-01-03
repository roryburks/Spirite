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

}