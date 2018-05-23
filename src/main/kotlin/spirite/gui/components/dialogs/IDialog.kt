package spirite.gui.components.dialogs

import spirite.base.brains.IMasterControl
import spirite.base.imageData.IImageWorkspace
import spirite.gui.components.dialogs.IDialog.FilePickType
import spirite.gui.components.dialogs.IDialog.FilePickType.*
import spirite.gui.components.dialogs.NewSimpleLayerPanel.NewSimpleLayerReturn
import spirite.gui.components.dialogs.NewWorkspacePanel.NewWorkspaceReturn
import spirite.gui.resources.Skin.Global
import spirite.gui.resources.SwIcons
import spirite.pc.gui.SColor
import spirite.pc.gui.basic.jcomponent
import spirite.pc.gui.jcolor
import spirite.pc.gui.scolor
import java.io.File
import javax.swing.JColorChooser
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

interface IDialog {
    fun invokeNewSimpleLayer( workspace: IImageWorkspace) : NewSimpleLayerReturn?
    fun invokeNewWorkspace(): NewWorkspaceReturn?

    enum class FilePickType {
        OPEN,
        SAVE_SIF,
        EXPORT,
        AAF
    }
    fun pickFile( type: FilePickType) : File?
    fun pickColor( defaultColor: SColor) : SColor?
}

class JDialog(private val master: IMasterControl) : IDialog
{

    init {
        UIManager.put("OptionPane.background", Global.Bg.color)
        UIManager.put("Panel.background", Global.Bg.color)
    }

    override fun invokeNewSimpleLayer(workspace: IImageWorkspace): NewSimpleLayerReturn? {
        val panel = NewSimpleLayerPanel(master,workspace)

        val result =JOptionPane.showConfirmDialog(
                null,
                panel.jcomponent,
                "New Layer",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                SwIcons.BigIcons.NewLayer.icon)

        return when(result) {
            JOptionPane.OK_OPTION -> panel.result
            else -> null
        }
    }

    override fun invokeNewWorkspace(): NewWorkspaceReturn? {
        val panel = NewWorkspacePanel(master)

        val result =JOptionPane.showConfirmDialog(
                null,
                panel.jcomponent,
                "New Layer",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                SwIcons.BigIcons.NewLayer.icon)

        return when(result) {
            JOptionPane.OK_OPTION -> panel.result
            else -> null
        }
    }


    override fun pickFile(type: FilePickType): File? {
        val fc = JFileChooser()

        val defaultFile = when( type) {
            OPEN -> master.settingsManager.openFilePath
            SAVE_SIF -> master.settingsManager.workspaceFilePath
            EXPORT -> master.settingsManager.imageFilePath
            AAF -> master.settingsManager.aafFilePath
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
        }
        filters.forEach { fc.addChoosableFileFilter(it) }

        fc.currentDirectory = defaultFile
        fc.selectedFile = defaultFile

        if( fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
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