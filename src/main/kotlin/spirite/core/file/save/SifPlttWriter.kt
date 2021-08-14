package spirite.core.file.save

import rb.file.IWriteStream
import spirite.core.file.contracts.SifPlttChunk

object SifPlttWriter {
    fun write(out: IWriteStream, data: SifPlttChunk) {
        data.palettes.forEach { palette ->
            out.writeStringUft8Nt(palette.name)
            out.writeShort(palette.raw.size)
            out.write(palette.raw)
        }
    }
}