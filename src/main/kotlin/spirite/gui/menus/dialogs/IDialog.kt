package spirite.gui.menus.dialogs

import cwShared.dialogSystem.IDialogPanel
import rb.glow.SColor
import sgui.swing.jcolor
import sgui.swing.scolor
import sgui.swing.skin.Skin.Global
import sguiSwing.components.jcomponent
import spirite.base.brains.dialog.IDialog
import spirite.base.brains.dialog.IDialog.FilePickType
import spirite.base.brains.dialog.IDialog.FilePickType.*
import spirite.base.brains.IMasterControl
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.ffa.FfaCascadingSublayerContract
import spirite.base.imageData.animation.ffa.FfaLayerCascading
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.brains.dialog.DisplayOptions
import spirite.gui.resources.SpiriteIcons
import java.io.File
import javax.swing.JColorChooser
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

class JDialog(private val master: IMasterControl) : IDialog
{
    init {
        UIManager.put("OptionPane.background", Global.Bg.jcolor)
        UIManager.put("Panel.background", Global.Bg.jcolor)
    }


    override fun promptForString(message: String, default: String): String? {
        val result: String?  = JOptionPane.showInputDialog(null, message, default)
        return result
    }

    override fun promptVerify(message: String): Boolean {
        return when( JOptionPane.showConfirmDialog(null, message,"",JOptionPane.OK_CANCEL_OPTION)){
            JOptionPane.OK_OPTION -> true
            else -> false
        }
    }
    override fun promptMessage(message: String) {
        JOptionPane.showConfirmDialog(null, message,"",JOptionPane.OK_OPTION)
    }

    override fun invokeNewSimpleLayer(workspace: IImageWorkspace)
            = runDialogPanel(NewSimpleLayerPanel(master,workspace))

    override fun invokeWorkspaceSizeDialog(description: String)
            = runDialogPanel(WorkspaceSizePanel(master))

    override fun invokeNewFfaCascadingLayerDetails(defaultInfo: FfaCascadingSublayerContract)
            = runDialogPanel(FfaCascadingLayerDetailsPanel(defaultInfo))

    override fun invokeDisplayOptions(title: String, default: DisplayOptions?)
            = runDialogPanel(DisplayOptionsPanel(default))

    override fun invokeNewFfaJsonImport(layer: FfaLayerCascading)
            = runDialogPanel(FfaCascadingJsonPanel(layer))

    override fun invokeMoveSpriteParts(parts: List<SpritePart>): SpriteLayer?
            = runDialogPanel(MoveSpritePartsPanel(parts))

    fun <T> runDialogPanel(panel: IDialogPanel<T>) = when(JOptionPane.showConfirmDialog(
            null,
            panel.jcomponent,
            "New Layer",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            SpiriteIcons.BigIcons.NewLayer.icon))
    {
        JOptionPane.OK_OPTION -> panel.result
        else -> null
    }

    override fun pickFile(type: FilePickType): File? {
        val fc = JFileChooser()

        val defaultFile = when( type) {
            OPEN -> master.settingsManager.openFilePath
            SAVE_SIF -> master.settingsManager.workspaceFilePath
            EXPORT -> master.settingsManager.imageFilePath
            AAF -> master.settingsManager.aafFilePath
            GIF -> master.settingsManager.gifFilePath
        }

        fc.choosableFileFilters.forEach { fc.removeChoosableFileFilter(it) }
        val filters = when(type) {
            OPEN, EXPORT -> listOf(
                    FileNameExtensionFilter("Supported Image Files", "jpg", "jpeg", "png", "bmp", "sif", "sif~"),
                    FileNameExtensionFilter("Spirite Workspace File", "sif"),
                    FileNameExtensionFilter("JPEG File", "jpg", "jpeg"),
                    FileNameExtensionFilter("PNG File", "png"),
                    FileNameExtensionFilter("Bitmap File", "bmp"),
                    fc.acceptAllFileFilter)
            SAVE_SIF -> listOf(
                    FileNameExtensionFilter("Spirite Workspace File", "sif"),
                    FileNameExtensionFilter("WARNING: To save as Image File use Export Option", "\u0000"),
                    fc.acceptAllFileFilter)
            AAF -> listOf(
                    FileNameExtensionFilter("AAF File", "aaf"),
                    FileNameExtensionFilter("PNG File", "png"),
                    fc.acceptAllFileFilter)
            GIF -> listOf(
                    FileNameExtensionFilter("GIF File", "gif"),
                    fc.acceptAllFileFilter)
        }
        filters.forEach { fc.addChoosableFileFilter(it) }

        fc.currentDirectory = defaultFile
        fc.selectedFile = defaultFile

        val result = when( type) {
            OPEN -> fc.showOpenDialog(null)
            else -> fc.showSaveDialog(null)
        }

        if( result == JFileChooser.APPROVE_OPTION) {
            var saveFile = fc.selectedFile

            if( type == SAVE_SIF && saveFile.name.indexOf('.') == -1)
                if( !File(saveFile.absolutePath + ".sif").exists() ||
                        JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Overwrite Existing ${saveFile.name}.sif?","", JOptionPane.YES_NO_OPTION))
                    saveFile = File(saveFile.absolutePath + ".sif")

            when( type) {
                OPEN, EXPORT -> {
                    when( saveFile.name.endsWith(".sif") || saveFile.name.endsWith(".sif~")) {
                        true -> master.settingsManager.workspaceFilePath = saveFile
                        else -> master.settingsManager.imageFilePath = saveFile
                    }
                }
                SAVE_SIF -> master.settingsManager.workspaceFilePath = saveFile
                AAF -> master.settingsManager.aafFilePath = saveFile
                GIF -> master.settingsManager.gifFilePath = saveFile
            }

            return saveFile
        }

        return null
    }

    override fun pickColor(defaultColor: SColor): SColor? {
        return JColorChooser.showDialog(
                null,
                "Choose Background Color",
                defaultColor.jcolor)?.scolor
    }
}