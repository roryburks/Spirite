package spirite.base.graphics

import spirite.base.imageData.IImageWorkspace
import spirite.base.util.linear.Transform
import spirite.hybrid.EngineLaunchpoint


/**
 * DynamicImage
 * 	A wrapper for a RawImage which automatically resizes itself to
 *
 *	Although much of this is simply splitting DynamicMedium's code over two objects, since
 *	other mediums commonly use this behavior, putting it in its own scope.
 */
class DynamicImage(
        raw: RawImage,
        xOffset: Int = 0,
        yOffset: Int = 0)
{
    var xOffset = xOffset ; private set
    var yOffset = yOffset ; private set
    var base: RawImage = raw ; private set

    val width = base.width
    val height = base.height

    fun drawToImage(drawer: (RawImage) -> Unit,
                    compositingWidth: Int,
                    compositingHeight: Int
                    tBaseToCompositing: Transform, useBaseOrientation: Boolean = false) {

    }

    // region Checkin/Checkout
    private var checkoutCount = 0
    private var workingTransform : Transform? = null
    private var buffer: RawImage? = null

    private fun checkoutRaw( transform: Transform) : RawImage {
        if( checkoutCount++ == 0) {
            workingTransform = transform
            buffer = EngineLaunchpoint.createImage(1,1)
            val gc = buffer!!.graphics
            //gc.drawImage(base, xOffset, yOffset)
        }
        return buffer!!
    }

    fun checkin() {
        if(--checkoutCount == 0) {
            val w = workspace.width
            val h = workspace.height

            val invertedTransform = workingTransform!!.invert()

        }
    }
    // endregion
}