package spirite.base.file.sif.v1.load

import rb.vectrix.mathUtil.i
import spirite.base.file.sif.SaveLoadUtil
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpritePartStructure
import spirite.base.imageData.mediums.MediumType
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.IDebug.WarningType.UNSUPPORTED
import spirite.core.util.StringUtil

interface ILayerLoader
{
    fun loadLayer(context : LoadContext, name: String) : Layer?
}

object LayerLoaderFactory
{
    fun getLayerLoader(version: Int, typeId: Int) : ILayerLoader {
        return  when( typeId ) {
            SaveLoadUtil.NODE_SIMPLE_LAYER -> SimpleLayerLoader
            SaveLoadUtil.NODE_SPRITE_LAYER -> when {
                version <= 4 -> LegacySpriteLayerLoader_0_4
                else -> SpriteLayerLoader
            }
            SaveLoadUtil.NODE_REFERENCE_LAYER -> ReferenceLayerIgnorer
            SaveLoadUtil.NODE_PUPPET_LAYER -> PuppetLayerIgnorer
            else -> throw BadSifFileException("Unrecognized GroupNode Type ID: $typeId (version mismatch or corrupt file?)")
        }
    }
}

object SimpleLayerLoader : ILayerLoader
{
    override fun loadLayer(context : LoadContext, name: String): Layer? {
        val mediumId = context.ra.readInt()

        return try {
            SimpleLayer(MediumHandle(context.workspace, context.reindex(mediumId)))
        } catch (e: Exception) {
            null
        }
    }
}

object LegacySpriteLayerLoader_0_4 : ILayerLoader
{
    override fun loadLayer(context: LoadContext, name: String): Layer? {
        val ra = context.ra
        val workspace = context.workspace

        val names = HashSet<String>()
        val partSize = ra.readUnsignedByte()
        val parts = List(partSize) {
            val origName = SaveLoadUtil.readNullTerminatedStringUTF8(ra)
            val partName = StringUtil.getNonDuplicateName(names, origName)
            names.add(partName)
            val transX = ra.readShort().toFloat()
            val transY = ra.readShort().toFloat()
            val drawDepth = ra.readInt()

            Pair( MediumHandle(workspace,context.reindex(ra.readInt())),
                    SpritePartStructure(drawDepth, partName, true, 1f, transX, transY, 1f, 1f, 0f))
        }

        return SpriteLayer(workspace, parts)
    }
}
object SpriteLayerLoader : ILayerLoader
{
    override fun loadLayer(context: LoadContext, name: String): Layer? {
        val ra = context.ra
        val workspace = context.workspace

        val type = if( context.version >= 0x1_000A) MediumType.fromCode(ra.readByte().i) ?: MediumType.DYNAMIC else MediumType.DYNAMIC

        val names = HashSet<String>()

        val partSize = ra.readUnsignedByte()
        val parts = List(partSize) {
            val origName = SaveLoadUtil.readNullTerminatedStringUTF8(ra)
            val partName = StringUtil.getNonDuplicateName(names, origName)
            names.add(partName)
            val transX = ra.readFloat()
            val transY = ra.readFloat()
            val scaleX = ra.readFloat()
            val scaleY = ra.readFloat()
            val rot = ra.readFloat()
            val drawDepth = ra.readInt()
            val handleId = ra.readInt()
            val alpha = if( context.version >= 0x1_0003) ra.readFloat() else 1f

            Pair( MediumHandle(workspace,context.reindex(handleId)),
                    SpritePartStructure(drawDepth, partName, true, alpha, transX, transY, scaleX, scaleY, rot))
        }
        return SpriteLayer(workspace, parts, type = type)
    }
}

object ReferenceLayerIgnorer : ILayerLoader
{
    override fun loadLayer(context: LoadContext, name: String): Layer? {
        DebugProvider.debug.handleWarning(UNSUPPORTED, "Reference Layers are currently not supported by Spirite v2, ignoring Refernce Layer")
        context.ra.readInt()    // [4] : NodeID
        return null
    }
}

object PuppetLayerIgnorer : ILayerLoader
{
    override fun loadLayer(context: LoadContext, name: String): Layer? {
        val ra = context.ra

        DebugProvider.debug.handleWarning(UNSUPPORTED, "Puppet Layers are currently not supported by Spirite v2, ignoring Puppet Layer")
        val byte = ra.readByte()   // [1] : Whether or not is derived
        if( byte.i == 0) {
            val numParts = ra.readUnsignedShort() // [2] : Number of parts
            for( i in 0 until numParts) {
                ra.readShort()  // [2] : Parent
                ra.readInt()    // [4] : MediumId
                ra.readFloat()  // [16] : Bone x1, y1, x2, y2
                ra.readFloat()
                ra.readFloat()
                ra.readFloat()
                ra.readInt()    // [4]: DrawDepth
            }

        }
        else throw BadSifFileException("Do not know how to handle derived puppet types")
        return null
    }
}