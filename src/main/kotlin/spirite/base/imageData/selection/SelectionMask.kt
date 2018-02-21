package spirite.base.imageData.selection

import spirite.base.graphics.IImage
import spirite.hybrid.ContentBoundsFinder
import spirite.hybrid.EngineLaunchpoint

class SelectionMask {
    val mask: IImage
    val ox: Int
    val oy: Int
    constructor( mask: IImage, ox: Int = 0, oy: Int = 0) {
        val cropped = ContentBoundsFinder.findContentBounds(mask, 1, true)

        val maskBeingBuilt = EngineLaunchpoint.createImage( cropped.width, cropped.height)
        maskBeingBuilt.graphics.renderImage(mask, -cropped.x, -cropped.y)

        this.mask = maskBeingBuilt
        this.ox = ox + cropped.x
        this.oy = oy + cropped.y
    }

    constructor( other: SelectionMask, ox: Int, oy: Int) {
        this.mask = other.mask
        this.ox = ox
        this.oy = oy
    }

    // region Lifting

    // TODO

    // endregion

}