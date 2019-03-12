package spirite.gui.views.work

import rb.extendo.delegates.DerivedLazy
import rb.extendo.delegates.OnChangeDelegate
import rb.owl.IObservable
import rb.owl.Observable
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF
import rb.vectrix.mathUtil.f
import spirite.base.imageData.IImageWorkspace

/** The view describes which part of the image is currently being seen and
 * manages conversions between the screen space and the image space. */
class WorkSectionView(val workspace: IImageWorkspace) {
    /** zoom_level 0 = 1x, 1 = 2x, 2 = 3x, ...
     *  -1 = 1/2x, -2 = 1/3x, -3 = 1/4x .... */
    var zoomLevel by ViewChange(0)
    val zoom get() = when {
        zoomLevel >= 0 -> zoomLevel + 1f
        else -> 1f / (-zoomLevel + 1f)
    }

    var offsetX by ViewChange(0)
    var offsetY by ViewChange(0)
    var rotation by ViewChange(0f)

    fun zoomIn() {
        zoomLevel = when {
            zoomLevel >= 11 -> (zoomLevel + 1) / 4 * 4 + 3
            zoomLevel >= 3 -> (zoomLevel+1) / 2 * 2 + 1
            else -> zoomLevel + 1
        }
    }
    fun zoomOut() {
        zoomLevel = when {
            zoomLevel > 11 -> zoomLevel / 4 * 4 - 1
            zoomLevel > 3 -> zoomLevel / 2 * 2 - 1
            else -> zoomLevel - 1
        }
    }

    private val tWorkspaceToScreenDerived = DerivedLazy {
        ImmutableTransformF.Translation(-offsetX.f, -offsetY.f) *
                ImmutableTransformF.Rotation(rotation) *
                ImmutableTransformF.Scale(zoom, zoom)
    }
    val tWorkspaceToScreen : ITransformF by tWorkspaceToScreenDerived

    private val tScreenToWorkspaceDerived = DerivedLazy {
        tWorkspaceToScreen.invert() ?: ImmutableTransformF.Identity
    }
    val tScreenToWorkspace : ITransformF by tScreenToWorkspaceDerived

    val viewObserver : IObservable<()->Unit> get() = _viewObserver
    private val _viewObserver = Observable<()->Unit>()

    private inner class ViewChange<T>(defaultValue : T) : OnChangeDelegate<T>(defaultValue, {
        tWorkspaceToScreenDerived.reset()
        tScreenToWorkspaceDerived.reset()
        _viewObserver.trigger { it.invoke() }
    })
}
