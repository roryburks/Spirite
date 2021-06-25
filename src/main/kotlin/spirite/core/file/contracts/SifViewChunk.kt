package spirite.core.file.contracts

class SifViewChunk(val views: SifViewView)

class SifViewView(
    val selectedNodeId: Int,
    val nodeProperties: List<Properties> )
{
    class Properties(
        val bitmap: Byte,
        val alpha: Float,
        val renderMethod: Byte,
        val renderValue: Int,
        val ox: Int,
        val oy: Int)
}