//package demonstration
//
//import sgui.generic.Orientation.VERTICAL
//import sgui.generic.components.ITreeView.*
//import sgui.generic.components.ITreeViewNonUI.*
//import sgui.swing.components.ResizeContainerPanel
//import sgui.swing.advancedComponents.SwTreeView
//import sgui.generic.components.IComponent
//import sgui.basic.basic.SwButton
//import sgui.basic.basic.SwLabel
//import sgui.basic.basic.jcomponent
//import java.awt.GridLayout
//import java.awt.datatransfer.DataFlavor
//import java.awt.datatransfer.Transferable
//import javax.swing.JFrame
//import javax.swing.SwingUtilities
//
//
//fun main( args: Array<String>) {
//
//    SwingUtilities.invokeLater {
//        val wsd = TreeDemo()
//        DemoLauncher.launch(wsd, 800, 600)
//    }
//}
//
//val i = 0
//    get() = field++
//
//class TreeDemo : JFrame() {
//    init {
//        layout = GridLayout()
//
//        val tree = SwTreeView<String>()
//        tree.addRoot("0", STNAtt())
//        tree.addRoot("1", STNAtt())
//        tree.addRoot("2", STNAtt())
//        tree.rootNodes[2].addChild("3", STNAtt())
//        tree.rootNodes[2].addChild("4", STNAtt())
//        tree.rootNodes[2].addChild("5", STNAtt())
//        tree.rootNodes[2].children[0].addChild("6", STNAtt())
//        tree.rootNodes[2].children[0].addChild("7", STNAtt())
//        tree.rootNodes[2].children[0].addChild("8", STNAtt())
//        tree.addRoot("9", STNAtt())
//        tree.leftSize = 30
//
//        val tree2 = SwTreeView<String>()
//        tree2.constructTree {
//            GroupNode("A", STNAtt())
//            GroupNode("B", STNAtt())
//            Branch("C", STNAtt(), {
//                GroupNode("D", STNAtt())
//                GroupNode("E", STNAtt())
//                Branch("F", STNAtt(), {
//                    GroupNode("G", STNAtt())
//                    GroupNode("H", STNAtt())
//                    GroupNode("I", STNAtt())
//                })
//            })
//            GroupNode("J")
//        }
//
//        val resizeContainer = ResizeContainerPanel(tree, VERTICAL)
//        resizeContainer.addPanel(tree2, 100, 100)
//
//        this.add(resizeContainer.jcomponent)
//    }
//
//    class STNAtt() : ITreeNodeAttributes<String> {
//        override fun makeLeftComponent(t: String): IComponent? = SwButton("${i}")
//        override fun makeComponent(t: String): IComponent = SwLabel(t)
//
//        override fun canImport(trans: Transferable) = true
//        override fun interpretDrop(trans: Transferable, dropInto: ITreeNode<String>, dropDirection: DropDirection) {
//            println(trans.getTransferData(DataFlavor.stringFlavor))
//        }
//    }
//
//}