package cwShared.dialogSystem

import sguiSwing.components.jcomponent
import spirite.gui.resources.SpiriteIcons
import javax.swing.JOptionPane

class AbstractDialogRunner {

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
}