package spirite.base.graphics.rendering

import rb.glow.IGraphicsContext
import rb.glow.gle.RenderRubric
import rb.glow.img.RawImage
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.mathUtil.ceil
import rb.vectrix.mathUtil.d
import sgui.core.systems.IImageCreator
import spirite.base.graphics.isolation.IIsolator
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.TransformedHandle
import spirite.base.imageData.groupTree.GroupNode
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.mediums.IComplexMedium
import spirite.core.hybrid.DebugProvider
import spirite.core.hybrid.DiSet_Hybrid
import spirite.core.hybrid.IDebug
import spirite.core.hybrid.IDebug.ErrorType.STRUCTURAL

class NodeRenderer(
    val root: GroupNode,
    val workspace: IImageWorkspace,
    val settings: RenderSettings = RenderSettings(workspace.width, workspace.height, true),
    val rootIsolator: IIsolator? = null,
    private val _debug : IDebug = DebugProvider.debug,
    private val _imageCreator: IImageCreator = DiSet_Hybrid.imageCreator)
{
    private lateinit var buffer : Array<RawImage>
    private val neededImages : Int by lazy {
        root
                .getAllNodesSuchThat(
                    {it.isVisible && it !is GroupNode },
                    {it.isVisible})
                .map { it.getDepthFrom(root) }
                .max() ?: 0
    }

    private var tick = 0    // increases to construct the subDepth

    private val ratioW get() = settings.width.toFloat() / workspace.width.toFloat()
    private val ratioH get() = settings.height.toFloat() / workspace.height.toFloat()


    fun render( gc: IGraphicsContext) {
        try {
            buildCompositeLayer()

            // Step 1: Create needed data
            if( neededImages == 0) return

            try {
                buffer = Array(neededImages) { _imageCreator.createImage(settings.width, settings.height) }
                buffer.forEach { it.graphics.clear() }

                // Step 2: Recursively Draw the image
                _renderRec(root, 0, rootIsolator)
                gc.renderImage(buffer[0], 0.0, 0.0)
            }finally {
                buffer.forEach { it.flush() }
            }
        }finally {
            builtComposite?.compositeImage?.flush()
        }
    }

    private fun _renderRec(node: GroupNode, depth: Int, isolator: IIsolator?) {
        // Note: though it doesn't seem recursive at first glance, _getDrawListUnsorted can either be recursive itself
        //  or might add GroupDrawThing's which call _renderRec.
        if( depth < 0 || depth >= buffer.size) {
            _debug.handleError(STRUCTURAL, "NodeRenderer out of expected layers count.  Expected: [${0},${buffer.size}), Actual: $depth")
            return
        }

        val gc = buffer[depth].graphics

        _getDrawListUnsorted(node, depth, isolator)
                .sortedWith(compareBy({it.depth}, {it.subDepth}))
                .forEach { it.draw(gc) }
    }

    private fun _getDrawListUnsorted(node: GroupNode, depth: Int, isolator: IIsolator?, flat: Boolean = false ) : List<DrawThing>{
        if( depth < 0 || depth >= buffer.size) {
            _debug.handleError(STRUCTURAL, "NodeRenderer out of expected layers count.  Expected: [${0},${buffer.size}), Actual: $depth")
            return emptyList()
        }

        val drawList = mutableListOf<DrawThing>()

        // Go through the node's children (in reverse), drawing any visible group
        //	found recursively and drawing any Layer found plainly.
        node.children.asReversed().asSequence()
                // Note: The second half is needed to attempts to render children at max_depth+1 when it's already been
                //  determined that there are no children  there (hence why the max_depth was set lower)
                .filter { it.isVisible && !(depth == buffer.size-1 && it is GroupNode) }
                .forEach { child ->
                    val subIsolator = isolator?.getIsolatorForNode(child)
                    if(subIsolator?.isDrawn != false) {
                        when (child) {
                            is GroupNode -> {
                                when {
                                    flat || child.flattened -> drawList.addAll(_getDrawListUnsorted(child, depth + 1, subIsolator, true))
                                    else -> drawList.add(GroupDrawThing(depth, child, subIsolator))
                                }
                            }
                            is LayerNode ->
                                child.layer.getDrawList(isolator).forEach { drawList.add(TransformedDrawThing(child, it, isolator)) }
                        }
                    }
                }

        return drawList
    }

    // region Composite Layer
    private class BuiltComposite(
            val handle: MediumHandle,
            val compositeImage: RawImage,
            val tCompositeToMedium : ITransformF)

    private var builtComposite: BuiltComposite? = null

    private fun buildCompositeLayer() {
        val compositeSource = workspace.compositor.compositeSource
        val lifted = workspace.selectionEngine.liftedData

        if( compositeSource != null || lifted != null) {
            val active = compositeSource?.arranged ?: workspace.activeData ?: return
            val medium = active.handle.medium
            val built = medium.build(active)

            // Should be killed by NodeRenderer at the end of render
            val compositeImage = _imageCreator.createImage(
                    (built.width*ratioW).ceil,
                    (built.height*ratioH).ceil)
            val gc = compositeImage.graphics
            val baseTransform = ImmutableTransformF.Scale(ratioW, ratioH)
            gc.transform = baseTransform


            // Draw the base
            gc.preTransform( built.tMediumToComposite)
            if(compositeSource?.drawsSource != false) {
                when (medium) {
                    is IComplexMedium -> medium.drawBehindComposite(gc)
                    else -> medium.render(gc)
                }
            }

            gc.pushTransform()


            // Draw the lifted image
            if( lifted != null) {
                gc.transform = built.tWorkspaceToComposite

                val selectionTransform =  workspace.selectionEngine.selectionTransform
                val proposingTransform =  workspace.selectionEngine.proposingTransform
                val toTrans = when {
                    proposingTransform == null -> selectionTransform
                    selectionTransform == null -> proposingTransform
                    else -> selectionTransform * proposingTransform
                }
                toTrans?.also { gc.preTransform(it) }
                lifted.draw(gc)
            }

            // Draw the composite
            if( compositeSource != null) {
                gc.transform = baseTransform
                compositeSource.drawer.invoke(gc)
            }

            gc.popTransform()

            // Draw over the composite
            if(compositeSource?.drawsSource != false && medium is IComplexMedium)
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
        abstract fun draw( gc: IGraphicsContext)
    }

    private inner class GroupDrawThing(
        val n: Int,
        val node: GroupNode,
        val isolator: IIsolator?,
        override val depth: Int = 0)
        : DrawThing()
    {
        override fun draw(gc: IGraphicsContext) {
            buffer[n+1].graphics.clear()

            _renderRec(node, n+1, isolator)

            val rubric = RenderRubric(node.tNodeToContext, node.alpha, node.method)
            gc.renderImage( buffer[n+1], 0.0, 0.0, rubric)
        }
    }

    private inner class TransformedDrawThing(
        val node: Node,
        val th : TransformedHandle,
        val isolator: IIsolator?)
        : DrawThing()
    {
        override val depth: Int get() = th.drawDepth

        override fun draw(gc: IGraphicsContext) {
            gc.pushTransform()
            gc.scale(ratioW.d, ratioH.d)

            val nodeTransformedRubric = th.renderRubric.stack(RenderRubric(node.tNodeToContext, node.alpha, node.method))
            val isolatorRubric = isolator?.rubric
            val baseRubric = if( isolatorRubric == null) nodeTransformedRubric else nodeTransformedRubric.stack(isolatorRubric)

            val builtComposite = builtComposite
            when(th.handle) {
                builtComposite?.handle -> {
                    val compositeRubric = baseRubric.stack(RenderRubric(transform = builtComposite.tCompositeToMedium))
                    gc.renderImage( builtComposite.compositeImage, 0.0, 0.0, compositeRubric)

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