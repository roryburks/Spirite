package demonstration

import spirite.gui.components.advanced.ITreeView.DropDirection
import spirite.gui.components.advanced.ITreeView.TreeNodeAttributes
import spirite.gui.components.advanced.SwTreeView
import spirite.gui.components.basic.IComponent
import spirite.pc.gui.basic.SwButton
import spirite.pc.gui.basic.SwLabel
import spirite.pc.gui.basic.jcomponent
import java.awt.GridLayout
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.JFrame
import javax.swing.SwingUtilities


fun main( args: Array<String>) {

    SwingUtilities.invokeLater {
        val wsd = TreeDemo()
        DemoLauncher.launch(wsd, 800, 600)
    }
}

val i = 0
    get() = field++

class TreeDemo : JFrame() {
    init {
        layout = GridLayout()

        val tree = SwTreeView<String>()
        tree.addRoot("0", STNAtt())
        tree.addRoot("1", STNAtt())
        tree.addRoot("2", STNAtt())
        tree.rootNodes[2].addChild("3", STNAtt())
        tree.rootNodes[2].addChild("4", STNAtt())
        tree.rootNodes[2].addChild("5", STNAtt())
        tree.rootNodes[2].children[0].addChild("6", STNAtt())
        tree.rootNodes[2].children[0].addChild("7", STNAtt())
        tree.rootNodes[2].children[0].addChild("8", STNAtt())
        tree.addRoot("9", STNAtt())
        tree.leftSize = 30

        this.add(tree.jcomponent)
    }

    class STNAtt() : TreeNodeAttributes<String> {
        override fun makeLeftComponent(t: String): IComponent? = SwButton("${i}")
        override fun makeComponent(t: String): IComponent = SwLabel(t)

        override fun canImport(trans: Transferable) = true
        override fun interpretDrop(trans: Transferable, dropDirection: DropDirection) {
            println(trans.getTransferData(DataFlavor.stringFlavor))
        }
    }

}