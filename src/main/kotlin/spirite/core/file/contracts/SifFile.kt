package spirite.core.file.contracts

data class SifFile(
    val width: Int,
    val height: Int,
    val version: Int, // Ignored on write
    val grptTrunk: SifGrptChunk,
    val imgdChunk: SifImgdChunk,
    val animChink: SifAnimChunk,
    val plttChunk: SifPlttChunk,
    val tpltChunk: SifTpltChunk,
    val anspChunk: SifAnspChunk,
    val viewChunk: SifViewChunk )

data class SifFileChunk(
    val identifier: String,
    val pointer: Long,
    val size: Int,
    val data: Any)

