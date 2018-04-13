package spirite.base.imageData.layers

import spirite.base.graphics.rendering.TransformedHandle
import spirite.base.imageData.MMediumRepository
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.drawer.IImageDrawer

interface Layer {
    val x: Int
    val y: Int
    val width: Int
    val height: Int
    val activeData: ArrangedMediumData
    fun getDrawer( arranged: ArrangedMediumData): IImageDrawer
    val imageDependencies: List<MediumHandle>
    fun getDrawList() : List<TransformedHandle>
    fun dupe( mediumRepo: MMediumRepository) : Layer

    fun triggerChange() {

    }
}