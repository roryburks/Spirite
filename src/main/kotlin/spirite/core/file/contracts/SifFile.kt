package spirite.core.file.contracts

data class SifFile(
    val width: Int,
    val height: Int,
    val version: Int, // Ignored on write
    val chunks: List<SifFileChunk>)
{
}

data class SifFileChunk(
    val identifier: String,
    val pointer: Long,
    val size: Int,
    val data: Any)

