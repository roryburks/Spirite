package spirite.base.graphics.rendering

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.graphics.RenderRubric
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.mediums.IComplexMedium
import spirite.base.util.MathUtil
import spirite.base.util.linear.Transform
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

class NodeRenderer(
        val root: GroupNode,
        val workspace: IImageWorkspace,
        val settings: RenderSettings = RenderSettings(workspace.width, workspace.height, true))
{
    private lateinit var buffer : Array<RawImage>
    private val neededImages : Int by lazy {
        root
                .getAllNodesSuchThat(
                    {it.isVisible && it !is GroupNode},
                    {it.isVisible})
                .map { it.getDepthFrom(root) }
                .max() ?: 0
    }

    private var tick = 0    // increases to construct the subDepth

    private val ratioW get() = settings.width.toFloat() / workspace.width.toFloat()
    private val ratioH get() = settings.height.toFloat() / workspace.height.toFloat()


    fun render( gc: GraphicsContext) {
        try {
            buildCompositeLayer()

            // Step 1: Create needed data
            if( neededImages == 0) return

            buffer = Array(neededImages, {Hybrid.imageCreator.createImage(settings.width, settings.height)})
            buffer.forEach { it.graphics.clear() }

            // Step 2: Recursively Draw the image
            _renderRec(root, 0)
            gc.renderImage( buffer[0], 0, 0)
        }finally {
            // Flush the data
            buffer.forEach { it.flush() }
            builtComposite?.compositeImage?.flush()
        }
    }

    private fun _renderRec(node: GroupNode, depth: Int) {
        // Note: though it doesn't seem recursive at first glance, _getDrawListUnsorted can either be recursive itself
        //  or might add GroupDrawThing's which call _renderRec.
        if( depth < 0 || depth >= buffer.size) {
            MDebug.handleError(STRUCTURAL, "NodeRenderer out of expected layer count.  Expected: [${0},${buffer.size}), Actual: $depth")
            return
        }

        val gc = buffer[depth].graphics

        _getDrawListUnsorted(node, depth)
                .sortedWith(compareBy({it.depth}, {it.subDepth}))
                .forEach { it.draw(gc) }
    }

    private fun _getDrawListUnsorted(node: GroupNode, depth: Int) : List<DrawThing>{
        if( depth < 0 || depth >= buffer.size) {
            MDebug.handleError(STRUCTURAL, "NodeRenderer out of expected layer count.  Expected: [${0},${buffer.size}), Actual: $depth")
            return emptyList()
        }

        val drawList = mutableListOf<DrawThing>()

        // Go through the node's children (in reverse), drawing any visible group
        //	found recursively and drawing any Layer found plainly.
        node.children.asReversed()
                // Note: The second half is needed to attempts to render children at max_depth+1 when it's already been
                //  determined that there are no children  there (hence why the max_depth was set lower)
                .filter { it.isVisible && !(depth == buffer.size-1 && it is GroupNode) }
                .forEach { child ->
                    when( child) {
                        is GroupNode -> {
                            when {
                                child.flatenned -> drawList.addAll( _getDrawListUnsorted(child, depth+1))
                                else -> drawList.add( GroupDrawThing(depth, child))
                            }
                        }
                        is LayerNode ->
                            child.layer.getDrawList().forEach { drawList.add(TransformedDrawThing(child, it)) }
                    }
                }

        return drawList
    }

    // region Composite Layer
    private class BuiltComposite(
            val handle: MediumHandle,
            val compositeImage: RawImage,
            val tCompositeToMedium : Transform)

    private var builtComposite: BuiltComposite? = null

    private fun buildCompositeLayer() {
        val compositeSource = workspace.compositor.compositeSource
        val lifted = workspace.selectionEngine.liftedData

        if( compositeSource != null || lifted != null) {
            val active = compositeSource?.arranged ?: workspace.activeData ?: return
            val medium = active.handle.medium
            val built = medium.build(active)

            // Should be killed by NodeRenderer at the end of render
            val compositeImage = Hybrid.imageCreator.createImage(
                    MathUtil.ceil(built.width*ratioW),
                    MathUtil.ceil(built.height*ratioH))
            val gc = compositeImage.graphics
            val baseTransform = Transform.ScaleMatrix(ratioW, ratioH)
            gc.transform = baseTransform


            // Draw the base
            gc.preTransform( built.tMediumToComposite)
            when( medium) {
                is IComplexMedium -> medium.drawBehindComposite(gc)
                else -> medium.render(gc)
            }

            gc.pushTransform()


            // Draw the lifted image
            if( lifted != null) {
                gc.transform = built.tWorkspaceToComposite
                workspace.selectionEngine.selectionTransform?.apply { gc.preTransform(this)}
                lifted.draw(gc)
            }

            // Draw the composite
            if( compositeSource != null) {
                gc.transform = baseTransform
                compositeSource.drawer.invoke(gc)
            }

            gc.popTransform()

            // Draw over the composite
            if( medium is IComplexMedium)
                medium.drawOverComposite(gc)

            builtComposite = BuiltComposite(
                    active.handle,
                    compositeImage,
                    built.tCompositeToMedium)
        }
    }
    // endregion

    private abstract inner class DrawThing {
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

            val rubric = RenderRubric( node.tNodeToContext, node.alpha, node.method)
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
            gc.pushTransform()
            gc.scale(ratioW, ratioH)

            val baseRubric = th.renderRubric.stack(
                    RenderRubric(node.tNodeToContext, node.alpha, node.method))

            when(th.handle) {
                builtComposite?.handle -> {
                    val compositeRubric = baseRubric.stack(RenderRubric(transform = builtComposite!!.tCompositeToMedium))
                    gc.renderImage( builtComposite!!.compositeImage, 0, 0, compositeRubric)

                }
                else -> {
                    val rubric = th.renderRubric.stack(
                            RenderRubric(node.tNodeToContext, node.alpha, node.method))
                    th.handle.medium.render(gc, rubric)
                }
            }
            gc.popTransform()


        }
    }
}