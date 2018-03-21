package spirite.gui.components.dialogs

import spirite.base.brains.MasterControl
import spirite.base.imageData.IImageWorkspace
import spirite.gui.components.dialogs.NewSimpleLayerPanel.NewSimpleLayerReturn
import spirite.gui.resources.SwIcons
import spirite.pc.gui.basic.jcomponent
import javax.swing.JOptionPane

interface IDialog {
    fun invokeNewSimpleLayer( workspace: IImageWorkspace) : NewSimpleLayerReturn?
}

class Dialog(private val master: MasterControl) : IDialog
{
    override fun invokeNewSimpleLayer(workspace: IImageWorkspace): NewSimpleLayerReturn? {
        val panel = NewSimpleLayerPanel(master,workspace)

        val result = JOptionPane.showConfirmDialog(
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
}