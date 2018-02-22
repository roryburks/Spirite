package spirite.base.imageData.mediums

import spirite.base.imageData.MediumHandle
import spirite.base.util.linear.MutableTransform

class BuildingMediumData(
        val handle: MediumHandle,
        var mediumTransform: MutableTransform = MutableTransform.IdentityMatrix()

) {
    var color: Int = 0

    constructor(handle: MediumHandle, ox: Float, oy: Float ) : this( handle, MutableTransform.TranslationMatrix(ox, oy))

    fun doOnBuildData( doer : (BuiltMediumData) -> Unit) {
        doer.invoke( handle.medium.build(this))
    }
}