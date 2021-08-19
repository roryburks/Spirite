package spirite.base.brains.dialog

import rb.glow.SColor
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.ffa.FfaCascadingSublayerContract
import spirite.base.imageData.animation.ffa.FfaLayerCascading
import spirite.base.imageData.layers.sprite.SpriteLayer
import java.io.File

interface IDialog {
    fun invokeNewSimpleLayer( workspace: IImageWorkspace) : NewSimpleLayerReturn?
    fun invokeWorkspaceSizeDialog(description: String): WorkspaceSizeReturn?
    fun invokeNewFfaCascadingLayerDetails(defaultInfo: FfaCascadingSublayerContract) : FfaCascadingSublayerContract?
    fun invokeNewFfaJsonImport(layer: FfaLayerCascading) : List<FfaCascadingSublayerContract>?
    fun invokeMoveSpriteParts(parts: List<SpriteLayer.SpritePart>) : SpriteLayer?

    fun invokeDisplayOptions(title: String = "Display Options", default: DisplayOptions? = null) : DisplayOptions?

    fun promptForString( message: String, default: String = "") : String?
    fun promptVerify(message: String) : Boolean
    fun promptMessage(message: String)

    enum class FilePickType {
        OPEN,
        SAVE_SIF,
        EXPORT,
        AAF,
        GIF
    }
    fun pickFile( type: FilePickType) : File?
    fun pickColor( defaultColor: SColor) : SColor?
}