package spirite.core.file.contracts

class SifAnspChunk (val spaces: List<SifAnspSpace>)

class SifAnspSpace(
    val name: String,
    val anims: List<SifAnspAnim>,
    val links: List<SifAnspLink>)
class SifAnspAnim(
    val animId: Int,
    val endLinkAnimId: Int,
    val onEndFrame: Int?,
    val logicalX: Int,
    val logicalY: Int )
class SifAnspLink(
    val origAnimId: Int,
    val origFrame: Int,
    val destAnimId: Int,
    val destFrame: Int)