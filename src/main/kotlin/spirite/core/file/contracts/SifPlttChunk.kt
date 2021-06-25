package spirite.core.file.contracts

class SifPlttChunk (val palettes: List<SifPlttPalette>)

class SifPlttPalette(
    val name: String,
    val raw: ByteArray )