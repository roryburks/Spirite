package spirite.base.graphics.rendering

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.util.linear.Transform.Companion
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

    var tick = 0    // increases to construct the subDepth

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
                // Note: The second half is needed to attempts to render children at max_depth+1 when it's already been
                //  determined that there are no children  there (hence why the max_depth was set lower)
            .filter { it.render.isVisible && !(depth == buffer.size-1 && it is GroupNode) }
            .forEach {
                when( it) {
                    is GroupNode -> {
                        if( it.flatenned) {

                        }
                    }
                }
            }

    }

    private fun _getDrawList( node: GroupNode, depth: Int) : List<DrawThing>{
        val drawList = mutableListOf<DrawThing>()

        // Go through the node's children (in reverse), drawing any visible group
        //	found recursively and drawing any Layer found plainly.
        node.children.asReversed()
                // Note: The second half is needed to attempts to render children at max_depth+1 when it's already been
                //  determined that there are no children  there (hence why the max_depth was set lower)
                .filter { it.render.isVisible && !(depth == buffer.size-1 && it is GroupNode) }
                .forEach {
                    when( it) {
                        is GroupNode -> {
                            when {
                                it.flatenned -> drawList.addAll( _getDrawList(it, depth+1))
                                else -> drawList.add( GroupDrawThing(depth, it))
                            }
                        }
                        is LayerNode -> {

                        }
                    }
                }

        return drawList
    }

    // region Composite Layer
    var compositeHandle : MediumHandle? = null
    // endregion

    private abstract inner class DrawThing() {
        val subDepth: Int = tick++
        abstract val depth: Int
        abstract fun draw( gc:GraphicsContext)
    }

    private inner class GroupDrawThing(
            val n: Int,
            val node: GroupNode,
            override val depth: Int = 0)
        : DrawThing()
    {
        override fun draw(gc: GraphicsContext) {
            buffer[n+1].graphics.clear()

            _renderRec(node, n+1)

            val rubric = RenderRubric( Companion.TranslationMatrix(node.x + 0f, node.y + 0f), node.render.alpha, node.render.method)
            gc.renderImage( buffer[n+1], 0, 0, rubric)
        }
    }

    private inner class TransformedDrawThing(
            val node: Node,
            val th : TransformedHandle)
        : DrawThing()
    {
        override val depth: Int get() = th.drawDepth

        override fun draw(gc: GraphicsContext) {
            gc.pushState()

            when(th.handle) {
                compositeHandle -> {}
                else -> {

                }
            }

            gc.popState()
        }

    }



}