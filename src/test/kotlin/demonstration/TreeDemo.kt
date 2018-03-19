package demonstration

import spirite.gui.components.advanced.SwTreeView
import spirite.gui.components.advanced.SwTreeView.TreeNodeAttributes
import spirite.gui.components.basic.IComponent
import spirite.pc.gui.basic.SwLabel
import spirite.pc.gui.basic.jcomponent
import java.awt.GridLayout
import java.awt.datatransfer.Transferable
import javax.swing.JFrame
import javax.swing.SwingUtilities


fun main( args: Array<String>) {

    SwingUtilities.invokeLater {
        val wsd = TreeDemo()
        DemoLauncher.launch(wsd, 800, 600)
    }
}

class TreeDemo : JFrame() {
    init {
        layout = GridLayout()

        val tree = SwTreeView<String>()
        tree.addRoot("0", STNAtt())
        tree.addRoot("1", STNAtt())
        tree.addRoot("2", STNAtt())
        tree._rootNodes[2].addChild("3", STNAtt())
        tree._rootNodes[2].addChild("4", STNAtt())
        tree._rootNodes[2].addChild("5", STNAtt())
        tree.addRoot("6", STNAtt())

        this.add(tree.jcomponent)
    }

    class STNAtt() : TreeNodeAttributes<String> {
        override val leftComponentBuilder: ((String) -> IComponent?)? get() = null
        override val componentBuilder: (String) -> IComponent = {SwLabel(it)}

        override fun canImport(trans: Transferable)= false
    }

}