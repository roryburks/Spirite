package spirite.gui.views.groupView

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.ffa.FfaLayerLexical
import spirite.base.imageData.animation.ffa.FfaLayerLexicalHelper
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node

object GroupViewHelper {
    fun determineLittleLabelText( master : IMasterControl, node: Node)  : String{
        val workspace = master.workspaceSet.currentMWorkspace ?: return ""
        if( node is GroupNode){
            val flags = mutableListOf<String>()
            if( node.flattened)
                flags.add("FLAT")

            val selected = workspace?.viewSystem?.animScrollViewModule?.selectedGroups ?: emptyList()
            if( selected.contains(node))
                flags.add("A")

            return flags.joinToString("-")
        }
        else if( node is LayerNode) {
            val parent = node.parent ?: return ""
            val anim = workspace.animationManager.currentAnimation as? FixedFrameAnimation ?: return  ""
            val layer = anim.layers
                .filterIsInstance<FfaLayerLexical>()
                .firstOrNull { it.groupLink == parent } ?: return  ""
            val lex = FfaLayerLexicalHelper.getLexiconOfNode(layer, node)
            return if( lex == null) "-" else "$lex"
        }

        return ""
    }
}