package spirite.core.file.contracts

class SifViewChunk(val views: List<SifViewView>)

class SifViewView(
    val selectedNodeId: Int,
    val nodeProperties: List<Properties> )
{
    class Properties(
        val bitmap: Byte,
        val alpha: Float,
        val renderMethod: Int,
        val renderValue: Int,
        val ox: Int,
        val oy: Int)
}