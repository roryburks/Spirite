package spirite.base.file.sif.v1.load

import rb.vectrix.mathUtil.i
import spirite.base.file.sif.SaveLoadUtil
import spirite.base.graphics.DynamicImage
import spirite.base.imageData.mediums.DynamicMedium
import spirite.base.imageData.mediums.FlatMedium
import spirite.base.imageData.mediums.IMedium
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.IDebug.WarningType.UNSUPPORTED
import spirite.sguiHybrid.Hybrid

interface IMediumLoader
{
    fun loadMedium(context : LoadContext) : IMedium?
}

object MediumLoaderFactory
{
    fun getMediumLoader( version: Int, typeId: Int) : IMediumLoader = when(typeId)
    {
        SaveLoadUtil.MEDIUM_PLAIN -> FlatMediumLoader
        SaveLoadUtil.MEDIUM_DYNAMIC -> DynamicMediumLoader
        SaveLoadUtil.MEDIUM_PRISMATIC -> PrismaticMediumIgnorer
        SaveLoadUtil.MEDIUM_MAGLEV -> when {
            version < 0x1_0000 -> Legacy_pre_1_0000_MaglevMediumLoader
            version <= 0x1_0006 -> Legacy_1_0006_MagneticMediumPartialLoader
            else -> MagneticMediumLoader_V2
        }
        else -> { throw BadSifFileException("Unrecognized Medium Type Id: $typeId.  Trying to load a newer SIF version in an older program version or corrupt file.") }
    }
}

object FlatMediumLoader : IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val imgSize = context.ra.readInt()
        val imgData = ByteArray(imgSize).apply { context.ra.read( this) }

        val img = Hybrid.imageIO.loadImage(imgData)
        return FlatMedium(img, context.workspace.mediumRepository)
    }
}

object DynamicMediumLoader : IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val ra = context.ra
        val ox = ra.readShort().i
        val oy = ra.readShort().i
        val imgSize = ra.readInt()
        val img = when( imgSize) {
            0 -> null
            else -> {
                val imgData = ByteArray(imgSize).apply { ra.read( this) }
                Hybrid.imageIO.loadImage(imgData)
            }
        }
        return DynamicMedium(context.workspace, DynamicImage(img, ox, oy))
    }
}

object PrismaticMediumIgnorer :IMediumLoader
{
    override fun loadMedium(context: LoadContext): IMedium? {
        val ra = context.ra

        DebugProvider.debug.handleWarning(UNSUPPORTED, "Prismatic Mediums are currently not supported by Spirite v2, ignoring.")
        val numlayers = ra.readUnsignedShort()
        repeat(numlayers) {
            ra.readInt()
            ra.readShort()
            ra.readShort()
            val imgSize = ra.readInt()
            ra.skipBytes(imgSize)
        }
        return null
    }
}