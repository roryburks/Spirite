package spirite.base.imageData

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.GraphicsContext.Composite
import spirite.base.imageData.IImageObservatory.ImageChangeEvent
import spirite.base.imageData.mediums.IMedium
import spirite.base.util.groupExtensions.SinglySet
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
        val id: Int)
{
    val width: Int get() = medium.width
    val height: Int get() = medium.height

    val medium : IMedium get() = workspace.mediumRepository.getData(id)!!

    fun refresh() {
        workspace.imageObservatory.triggerRefresh(
                ImageChangeEvent(
                        SinglySet(this),
                        emptySet(),
                        workspace))
    }
}