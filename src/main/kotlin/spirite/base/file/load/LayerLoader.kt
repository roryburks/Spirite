package spirite.base.file.load

import spirite.base.file.BadSifFileException
import spirite.base.file.LoadContext
import spirite.base.file.SaveLoadUtil
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.layers.Layer
import spirite.base.imageData.layers.SimpleLayer
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpritePartStructure
import spirite.base.util.i
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.UNSUPPORTED
import java.io.RandomAccessFile

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
            else -> throw BadSifFileException("Unrecognized Node Type ID: $typeId (version mismatch or corrupt file?)")
        }
    }
}

object SimpleLayerLoader : ILayerLoader
{
    override fun loadLayer(context : LoadContext, name: String): Layer? {
        val mediumId = context.ra.readInt()

        return SimpleLayer(MediumHandle(context.workspace, context.reindex(mediumId)))
    }
}

object LegacySpriteLayerLoader_0_4 : ILayerLoader
{
    override fun loadLayer(context: LoadContext, name: String): Layer? {
        val ra = context.ra
        val workspace = context.workspace

        val partSize = ra.readUnsignedByte()
        val parts = List(partSize) {
            val partName = SaveLoadUtil.readNullTerminatedStringUTF8(ra)
            val transX = ra.readShort().toFloat()
            val transY = ra.readShort().toFloat()
            val drawDepth = ra.readInt()

            Pair( MediumHandle(workspace,context.reindex(ra.readInt())),
                    SpritePartStructure(drawDepth, partName, true, 1f, transX, transY, 1f, 1f, 0f))
        }

        return SpriteLayer(workspace, workspace.mediumRepository, parts)
    }
}
object SpriteLayerLoader : ILayerLoader
{
    override fun loadLayer(context: LoadContext, name: String): Layer? {
        val ra = context.ra
        val workspace = context.workspace

        val partSize = ra.readUnsignedByte()
        val parts = List(partSize) {
            val partName = SaveLoadUtil.readNullTerminatedStringUTF8(ra)
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
        return SpriteLayer(workspace, workspace.mediumRepository, parts)
    }
}

object ReferenceLayerIgnorer : ILayerLoader
{
    override fun loadLayer(context: LoadContext, name: String): Layer? {
        MDebug.handleWarning(UNSUPPORTED, "Reference Layers are currently not supported by Spirite v2, ignoring Refernce Layer")
        context.ra.readInt()    // [4] : NodeID
        return null
    }
}

object PuppetLayerIgnorer : ILayerLoader
{
    override fun loadLayer(context: LoadContext, name: String): Layer? {
        val ra = context.ra

        MDebug.handleWarning(UNSUPPORTED, "Puppet Layers are currently not supported by Spirite v2, ignoring Puppet Layer")
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