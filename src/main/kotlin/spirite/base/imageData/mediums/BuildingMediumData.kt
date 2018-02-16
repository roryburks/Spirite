package spirite.base.imageData.mediums

import spirite.base.imageData.MediumHandle
import spirite.base.util.linear.MutableTransform

class BuildingMediumData {
    val handle: MediumHandle
    var mediumTransform: MutableTransform
    var color: Int = 0

    constructor( handle: MediumHandle) {
        this.handle = handle
        mediumTransform = MutableTransform.IdentityMatrix()
    }
    constructor(handle: MediumHandle, ox: Float, oy: Float ) {
        this.handle = handle
        mediumTransform = MutableTransform.TranslationMatrix(ox, oy)
    }

    fun doOnBuildData( doer : (BuiltMediumData) -> Unit) {
        val medium = handle.workspace.mediumRepository.getData(handle.id)
        doer.invoke( medium.build(this))
    }
}