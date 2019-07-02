package spirite.base.imageData.selection

import rb.glow.GraphicsContext
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import spirite.base.imageData.IImageWorkspace

fun drawLiftedTransform(gc: GraphicsContext, workspace: IImageWorkspace) {
    val lifted = workspace.selectionEngine.liftedData

    if( lifted != null) {

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
}