package spirite.base.file.contracts

data class SifFileK(
        val width: Int,
        val height: Int,
        val version: Int)

data class SifFileChunkK(
        val identifier: String,
        val pointer: Long,
        val size: Int)

