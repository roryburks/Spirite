package spirite.gui.major.work

import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.Transform

/** The view describes which part of the image is currently being seen and
 * manages conversions between the screen space and the image space. */
class WorkSectionView {
    /** zoom_level 0 = 1x, 1 = 2x, 2 = 3x, ...
     *  -1 = 1/2x, -2 = 1/3x, -3 = 1/4x .... */
    var zoomLevel = 0
    val zoom get() = when {
        zoomLevel >= 0 -> zoomLevel + 1f
        else -> 1f / (-zoomLevel + 1f)
    }

    var offsetX = 0
    var offsetY = 0
    var rotation = 0f

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

    val tWorkspaceToScreen : Transform get() {
        val trans = MutableTransform.TranslationMatrix(-offsetX/2.0f, -offsetY/2.0f)
        trans.preScale( zoom, zoom)
        trans.preRotate( rotation)
        return trans
    }
}