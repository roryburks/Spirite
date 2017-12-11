package spirite.base.image_data.mediums.maglev

import spirite.base.graphics.GraphicsContext
import spirite.base.graphics.IImage
import spirite.base.image_data.selection.ALiftedData
import spirite.base.image_data.selection.SelectionMask
import spirite.hybrid.HybridHelper

class MaglevLiftedData(val medium: MaglevMedium, selection: SelectionMask) : ALiftedData() {
    val iox: Int = selection.ox
    val ioy: Int = selection.oy

    init {
        medium.Build()
    }

    override fun drawLiftedData(gc: GraphicsContext) {
        gc.drawImage(medium.builtImage!!.base, medium.builtImage!!.xOffset - iox, medium.builtImage!!.yOffset - ioy)
    }

    override fun getWidth(): Int {
        return medium.width
    }

    override fun getHeight(): Int {
        return medium.height
    }

    override fun readonlyAccess(): IImage {
        // Somewhat wasteful that we turn the existing built image into a new image
        //	with a little more padding, but simpler an interface perspective
        val img = HybridHelper.createImage(medium.width + medium.builtImage!!.xOffset - iox,
                medium.height + medium.builtImage!!.yOffset - ioy)

        img.graphics.drawImage(medium.builtImage!!.base, medium.builtImage!!.xOffset - iox, medium.builtImage!!.yOffset - ioy)
        return img
    }

}
