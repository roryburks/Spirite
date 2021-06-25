package spirite.core.file.contracts

class SifTpltChunk(
    val nodeMaps: List<SifTpltNodeMap>,
    val spritePartMaps: List<SifTpltSpritePartMap>)

class SifTpltNodeMap(
    val nodeId: Int,
    val belt: List<Int>)
class SifTpltSpritePartMap(
    val groupNodeId: Int,
    val partName: String,
    val belt: List<Int> )