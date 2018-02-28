package spirite.base.graphics.rendering

import javafx.scene.canvas.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

class NodeRenderer(
        val root: GroupNode,
        val settings: RenderSettings)
{
    private lateinit var buffer : Array<RawImage>
    private val neededImages : Int by lazy {
        root
                .getAllNodesSuchThat(
                    {it.render.isVisible && it !is GroupNode},
                    {it.render.isVisible})
                .map { it.getDepthFrom(root) }
                .max() ?: 0
    }

    val workspace get() = settings.renderSource.workspace

    val ratioW get() = settings.width / workspace.width
    val ratioH get() = settings.height / workspace.height

    fun render( gc: GraphicsContext) {
        try {
            buildCompositeLayer()

            // Step 1: Create needed data
            if( neededImages == 0) return

            buffer = Array(neededImages, {Hybrid.imageCreator.createImage(settings.width, settings.height)})
            buffer.forEach { it.graphics.clear() }

            // Step 2: Recursively Draw the image
            _renderRec(root, 0)

        }finally {
        }
    }

    fun buildCompositeLayer() {

    }

    fun _renderRec(node: GroupNode, depth: Int) {
        if( depth < 0 || depth >= buffer.size) {
            MDebug.handleError(STRUCTURAL, "NodeRenderer out of expected layer count.  Expected: [${0},${buffer.size}), Actual: $depth")
            return
        }

        // Go through the node's children (in reverse), drawing any visible group
        //	found recursively and drawing any Layer found plainly.
        node.children.asReversed()
            .filter { it.render.isVisible }
            .forEach {
                when( it) {
                    is GroupNode -> {
                        if( depth != buffer.size-1) {

                        }
                    }
                }
            }

    }


}