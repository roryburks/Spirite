package rbJvm.animo

import java.nio.charset.Charset

object FileUtil {
    fun strToByteArrayUTF8(str: String): ByteArray {
        val b = (str + 0.toChar()).toByteArray(Charset.forName("UTF-8"))

        // Convert non-terminating null characters to whitespace
        val nil: Byte = 0
        for (i in 0 until b.size - 1) {
            if (b[i] == nil)
                b[i] = 0x20
        }

        return b
    }
}
