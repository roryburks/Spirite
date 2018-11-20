package spirite.base.file.load

import spirite.base.file.SaveLoadUtil
import spirite.base.imageData.animation.Animation
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.UNSUPPORTED

interface IAnimationLoader
{
    fun loadAnimation(context : LoadContext, name: String) : Animation?
}

object AnimationLoaderFactory
{
    fun getAnimationLoader( version: Int, animationTypeId: Int) : IAnimationLoader  {
        return when( animationTypeId) {
            SaveLoadUtil.ANIM_FFA -> when( version) {
                in 0..7 -> LegacyFFALoader_X_To_7
                in 8..0x1_0000 -> LegacyFFALoader_8_TO_1_0000
                else -> FFALoader
            }
            SaveLoadUtil.ANIM_RIG ->RigAnimationIgnorer
            else -> throw BadSifFileException("Unrecognized Animation Type ID: $animationTypeId (version mismatch or corrupt file?)")
        }

    }
}

object RigAnimationIgnorer : IAnimationLoader
{
    override fun loadAnimation(context: LoadContext, name: String): Animation? {
        val ra = context.ra

        MDebug.handleWarning(UNSUPPORTED, "Rig Animations are currently not supported by Spirite v2, ignoring.")
        val numSprites = ra.readUnsignedShort()
        repeat(numSprites) {
            val nodeId = ra.readInt()
            val numParts = ra.readUnsignedShort()
            repeat(numParts) {
                val partName = SaveLoadUtil.readNullTerminatedStringUTF8(ra)
                val numKeyframes = ra.readUnsignedShort()
                repeat(numKeyframes) {
                    val t = ra.readFloat()
                    val tx = ra.readFloat()
                    val ty = ra.readFloat()
                    val sx = ra.readFloat()
                    val sy = ra.readFloat()
                    val rot = ra.readFloat()
                }
            }
        }

        return null
    }

}