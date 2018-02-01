package spirite.base.imageData

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.GraphicsContext.Composite
import spirite.base.graphics.IImage
import spirite.base.util.linear.Transform
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType


/**
 * A MediumHandle is a reference to a Medium which is being maintained by
 * an ImageWorkspace.  All Semi-permanent Mediums (other than those in the
 * UndoEngine) should be managed through Handles and with the Workspace,
 * otherwise their low-level Graphical resources will only be released when
 * the GC gets around to clearing them.
 *
 * Null-context MediumHandles can be created but their intended use is for
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
        val context: IImageWorkspace,
        val id: Int
)
{
    // These variables are essentially final, but may take a while to be set
    val width: Int; get() = context.getData(id).width ?: 1
    val height: Int; get() = context.getData(id).height ?: 1
    //val isDynamic: Boolean get() = context?.getData(id) is DynamicMedium
    val dynamicX: Int get() = context.getData(id).dynamicX ?: 0
    val dynamicY: Int get() = context.getData(id).dynamicY ?: 0

    /** Returns a null-context duplicate (just preserves the ID)  */
    fun dupe() = MutableHandle(null, id)


    /** Should only be used for reading/copying in things that need direct
     * access to the BufferedImage.
     *
     * RETURN VALUE SHOULD NEVER BE STORED LONG-TERM, if used for writing,
     * will not trigger proper Observers.  And probably other bad stuff
     * will happen if it sticks around in GC
     */
    fun deepAccess(): IImage? = context.getData(id).readOnlyAccess()

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
        if (context == null) {
            MDebug.handleWarning(WarningType.STRUCTURAL, "Tried to render a context-less image.")
            return
        }
        val ii = context!!.getData(id) ?: return

        //gc.drawHandle(this, ii.dynamicX, ii.dynamicY)
    }

    // !!! START BAD
//    fun drawBehindStroke(gc: GraphicsContext) {
//        if (context == null) {
//            MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.")
//            return
//        }
//        val ii = context!!.getData(id)
//        if (ii is PrismaticMedium) {
//            ii.drawBehind(gc, context!!.paletteManager.getActiveColor(0))
//        } else
//            gc.drawHandle(this, 0, 0)
//    }
//
//    fun drawInFrontOfStroke(gc: GraphicsContext) {
//        if (context == null) {
//            MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.")
//            return
//        }
//        val ii = context!!.getData(id)
//        (ii as? PrismaticMedium)?.drawFront(gc, context!!.paletteManager.getActiveColor(0))
//    }
//    // !!! END BAD
//
    fun refresh() {
        TODO("Not implemented")
        // Construct ImageChangeEvent and send it
//        val evt = ImageChangeEvent()
//        evt.workspace = context
//        evt.dataChanged.add(this)
//        context!!.triggerImageRefresh(evt)
    }
}