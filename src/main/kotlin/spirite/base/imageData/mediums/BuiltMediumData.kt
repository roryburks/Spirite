package spirite.base.imageData.mediums

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.RawImage
import spirite.base.util.linear.Transform
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType.STRUCTURAL

abstract class BuiltMediumData( val arranged: ArrangedMediumData) {
    // Composited Dimensions will be used (by HybridNodeRenderer and StrokeEngines), Dimensions of the underlying RawImage
    //  will likely not be used.
    /** width of the composited Image */
    abstract val width : Int
    /** height of the composited Image */
    abstract val height: Int

    abstract val tMediumToComposite: Transform
    val tCompositeToMedium: Transform by lazy { tMediumToComposite.invert() }
    abstract val tWorkspaceToComposite: Transform

    // Note: Can be changed to return Boolean if we want to be able to differentiate between image-changing calls and
    //  non-image-changing calls (assuming there's ever a need for such a thing)
    private var doing = false
    fun drawOnComposite(drawer: (GraphicsContext) -> Unit) {
        if( doing) {
            MDebug.handleError(STRUCTURAL, "Tried to recursively check-out Medium.")
            return
        }
        doing = true
        _drawOnComposite(drawer)
        doing = false
        arranged.handle.refresh()
    }

    fun rawAccessComposite(doer: (RawImage) -> Unit) {
        if( doing) {
            MDebug.handleError(STRUCTURAL, "Tried to recursively check-out Medium.")
            return
        }
        doing = true
        _rawAccessComposite(doer)
        doing = false

        arranged.handle.refresh()
    }

    protected abstract fun _drawOnComposite(doer: (GraphicsContext) -> Unit)
    protected abstract fun _rawAccessComposite(doer: (RawImage) -> Unit)

}