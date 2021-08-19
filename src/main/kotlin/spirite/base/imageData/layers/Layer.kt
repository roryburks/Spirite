package spirite.base.imageData.layers

import spirite.base.graphics.drawer.IImageDrawer
import spirite.base.graphics.isolation.IIsolator
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.TransformedHandle
import spirite.base.imageData.mediums.ArrangedMediumData

interface Layer {
    val x: Int
    val y: Int
    val width: Int
    val height: Int
    val activeData: ArrangedMediumData
    val allArrangedData : List<ArrangedMediumData>
    fun getDrawer( arranged: ArrangedMediumData): IImageDrawer

    val imageDependencies: List<MediumHandle>
    fun getDrawList(isolator: IIsolator? = null) : List<TransformedHandle>
    fun dupe( workspace: MImageWorkspace) : Layer
}