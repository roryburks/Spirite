package spirite.core.file.load.grpt

import rb.file.IReadStream
import spirite.core.file.contracts.SifGrptChunk

interface ISifGrptReader {
    fun read( read: IReadStream, endPointer: Long) : SifGrptChunk
}

