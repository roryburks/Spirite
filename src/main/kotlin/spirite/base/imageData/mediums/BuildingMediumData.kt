package spirite.base.imageData.mediums

import spirite.base.imageData.MediumHandle
import spirite.base.util.linear.MutableTransform
import spirite.base.util.linear.MutableTransform.Companion

class BuildingMediumData {
    val handle: MediumHandle
    var trans: MutableTransform
    var color: Int = 0

    constructor( handle: MediumHandle) {
        this.handle = handle
        trans = MutableTransform.IdentityMatrix()
    }
    constructor(handle: MediumHandle, ox: Float, oy: Float ) {
        this.handle = handle
        trans = MutableTransform.TranslationMatrix(ox, oy)
    }

    fun doOnBuildData( doer : (BuiltMediumData) -> Unit) {
        val medium = handle.context.mediumRepository.getData(handle.id)
        doer.invoke( medium.build(this))
    }
}