package spirite.base.imageData

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.GraphicsContext.Composite
import spirite.base.util.linear.Transform


/**
 * A MediumHandle is a reference to a Medium which is being maintained by
 * an ImageWorkspace.  All Semi-permanent Mediums (other than those in the
 * UndoEngine) should be managed through Handles and with the Workspace,
 * otherwise their low-level Graphical resources will only be released when
 * the GC gets around to clearing them.
 *
 * Null-workspace MediumHandles can be created but their intended use is for
 * the LoadEngine which loads the logical structure then links the ImageData,
 * to avoid navigating a complex or even cyclical hierarchy of references
 * but can presumably be used in a similar fashion, but this is discouraged.
 *
 * !!!! NOTE: Use .equals().  There should be little to no reason to ever
 * use `==` as it defeats the entire point of handles.
 *
// ImageWorkspace.getData should have constant-time access (being implemented
//	with a HashTable, so there's probably no point to remember the
//	CachedImage and doing so might lead to unnecessary bugs.
 */

class MutableHandle(
    var context: IImageWorkspace?,
    var id: Int
)

data class MediumHandle(
        val workspace: IImageWorkspace,
        val id: Int
)
{
    // These variables are essentially final, but may take a while to be set
    val width: Int; get() = workspace.mediumRepository.getData(id).width ?: 1
    val height: Int; get() = workspace.mediumRepository.getData(id).height ?: 1

    /** Returns a null-workspace duplicate (just preserves the ID)  */
    fun dupe() = MutableHandle(null, id)

    fun drawLayer(
            gc: GraphicsContext, transform: Transform, composite: Composite, alpha: Float) {
        val oldAlpha = gc.alpha
        val oldComposite = gc.composite

        gc.setComposite(composite, alpha * oldAlpha)
        drawLayer(gc, transform)
        gc.setComposite(oldComposite, oldAlpha)
    }


    fun drawLayer(gc: GraphicsContext, transform: Transform) {
        var transform = transform
        val ii = workspace.mediumRepository.getData(id) ?: return

        //gc.drawHandle(this, ii.dynamicX, ii.dynamicY)
    }

    // !!! START BAD
//    fun drawBehindStroke(gc: GraphicsContext) {
//        if (workspace == null) {
//            MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a workspace-less image.")
//            return
//        }
//        val ii = workspace!!.getData(id)
//        if (ii is PrismaticMedium) {
//            ii.drawBehind(gc, workspace!!.paletteManager.getActiveColor(0))
//        } else
//            gc.drawHandle(this, 0, 0)
//    }
//
//    fun drawInFrontOfStroke(gc: GraphicsContext) {
//        if (workspace == null) {
//            MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a workspace-less image.")
//            return
//        }
//        val ii = workspace!!.getData(id)
//        (ii as? PrismaticMedium)?.drawFront(gc, workspace!!.paletteManager.getActiveColor(0))
//    }
//    // !!! END BAD
//
    fun refresh() {
        //TODO("Not implemented")
        // Construct ImageChangeEvent and send it
//        val evt = ImageChangeEvent()
//        evt.workspace = workspace
//        evt.dataChanged.add(this)
//        workspace!!.triggerImageRefresh(evt)
    }
}