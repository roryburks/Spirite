package spirite.base.file.load

import rb.glow.gle.RenderMethod
import rb.glow.gle.RenderMethodType
import rb.vectrix.mathUtil.i
import spirite.base.imageData.view.NodeViewProperties
import spirite.gui.views.groupView.NodeProperties

object ViewChunkLoader {
    fun load(context: LoadContext) {
        val ra = context.ra
        val numViews = ra.readByte().i
        val viewSystem = context.workspace.viewSystem

        viewSystem.numActiveViews = numViews

        for(view in 0 until numViews) {
            val selected = context.nodes.getOrNull(ra.readInt())
            viewSystem.setCurrentNode(view, selected)

            for (node in context.nodes) {
                val bitmap = ra.readByte()
                val visible = (bitmap.i == 1)
                val alpha = ra.readFloat()
                val renderMethod = RenderMethodType.values()[ra.readInt()]
                val renderValue = ra.readInt()
                val ox = ra.readShort().i
                val oy = ra.readShort().i

                val props = NodeViewProperties(ox, oy, visible, alpha, RenderMethod(renderMethod, renderValue))
                viewSystem.set(node, props, view)
            }
        }
    }
}