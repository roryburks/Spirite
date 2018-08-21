package spirite.gui.components.advanced.omniContainer

import spirite.base.brains.SwFrameManager
import spirite.pc.gui.basic.jcomponent
import java.awt.Dialog.ModalityType.MODELESS
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.JDialog


class SwOmniDialog(
        val component: IOmniComponent,
        val context: SwFrameManager)
    : JDialog()
{
    init {
        add(component.component.jcomponent)
        setSize(300,300)
        modalityType = MODELESS
        pack()
        isVisible = true


        this.addWindowListener(object : WindowListener {
            override fun windowDeiconified(e: WindowEvent?) {}
            override fun windowClosing(e: WindowEvent?) {
                component.close()
                context.closed(this@SwOmniDialog)
            }
            override fun windowClosed(e: WindowEvent?) {}

            override fun windowActivated(e: WindowEvent?) {}
            override fun windowDeactivated(e: WindowEvent?) {}
            override fun windowOpened(e: WindowEvent?) {}
            override fun windowIconified(e: WindowEvent?) {}
        })
    }

}