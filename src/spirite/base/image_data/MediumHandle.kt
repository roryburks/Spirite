package spirite.base.image_data


import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.GraphicsContext.Composite
import spirite.base.graphics.IImage
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent
import spirite.base.image_data.mediums.DynamicMedium
import spirite.base.image_data.mediums.PrismaticMedium
import spirite.base.util.linear.MatTrans
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
class MediumHandle(
        context: ImageWorkspace?,
        id: Int
)
{
    // These variables are essentially final, but may take a while to be set
    var context: ImageWorkspace? = context
    var id = -1
    val width: Int; get() = context?.getWidthOf(id) ?: 1
    val height: Int; get() = context?.getHeightOf(id) ?: 1
    val isDynamic: Boolean get() = context?.getData(id) is DynamicMedium
    val dynamicX: Int
        get() {
            if (context == null) return 0
            val ii = context!!.getData(id)
            return ii.dynamicX
        }
    val dynamicY: Int
        get() {
            if (context == null) return 0
            val ii = context!!.getData(id)
            return ii.dynamicY
        }

    init {
        this.context = context
        this.id = id
    }

    /** Returns a null-context duplicate (just preserves the ID)  */
    fun dupe(): MediumHandle {
        return MediumHandle(null, this.id)
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is MediumHandle)
            return false
        val other = obj as MediumHandle?

        return this.context === other!!.context && this.id == other!!.id
    }

    /** Should only be used for reading/copying in things that need direct
     * access to the BufferedImage.
     *
     * RETURN VALUE SHOULD NEVER BE STORED LONG-TERM, if used for writing,
     * will not trigger proper Observers.  And probably other bad stuff
     * will happen if it sticks around in GC
     */
    fun deepAccess(): IImage? {
        // TODO: BAD
        return if (context == null) null else context!!.getData(id).readOnlyAccess()
    }

    fun drawLayer(
            gc: GraphicsContext, transform: MatTrans, composite: Composite, alpha: Float) {
        val oldAlpha = gc.alpha
        val oldComposite = gc.composite

        gc.setComposite(composite, alpha * oldAlpha)
        drawLayer(gc, transform)
        gc.setComposite(oldComposite, oldAlpha)
    }


    fun drawLayer(gc: GraphicsContext, transform: MatTrans?) {
        var transform = transform
        if (context == null) {
            MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.")
            return
        }
        val ii = context!!.getData(id) ?: return

        gc.pushTransform()

        gc.transform = MatTrans()
        gc.drawHandle(this, ii.dynamicX, ii.dynamicY)

        gc.popTransform()
    }

    // !!! START BAD
    fun drawBehindStroke(gc: GraphicsContext) {
        gc.pushTransform()
        gc.transform = MatTrans()
        if (context == null) {
            MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.")
            return
        }
        val ii = context!!.getData(id)
        if (ii is PrismaticMedium) {
            ii.drawBehind(gc, context!!.paletteManager.getActiveColor(0))
        } else
            gc.drawHandle(this, ii.dynamicX, ii.dynamicY)
        gc.popTransform()
    }

    fun drawInFrontOfStroke(gc: GraphicsContext) {
        gc.pushTransform()
        gc.transform = MatTrans()
        if (context == null) {
            MDebug.handleWarning(WarningType.STRUCTURAL, null, "Tried to render a context-less image.")
            return
        }
        val ii = context!!.getData(id)
        (ii as? PrismaticMedium)?.drawFront(gc, context!!.paletteManager.getActiveColor(0))
        gc.popTransform()
    }
    // !!! END BAD

    fun refresh() {
        // Construct ImageChangeEvent and send it
        val evt = ImageChangeEvent()
        evt.workspace = context
        evt.dataChanged.add(this)
        context!!.triggerImageRefresh(evt)
    }
}
