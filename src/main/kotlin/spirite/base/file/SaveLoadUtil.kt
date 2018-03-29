package spirite.base.file

import java.io.UnsupportedEncodingException

object SaveLoadUtil {
    val header : ByteArray get() =ByteArray(4).apply {
        System.arraycopy("SIFF".toByteArray(charset("UTF-8")), 0, this, 0, 4)
    }
    val version = 0x0001_0001
}