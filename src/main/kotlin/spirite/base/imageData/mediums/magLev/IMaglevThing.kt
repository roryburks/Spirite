package spirite.base.imageData.mediums.magLev

import rb.glow.SColor
import rb.vectrix.linear.Vec3f
import spirite.base.imageData.mediums.BuiltMediumData


interface IMaglevThing {
    fun dupe() : IMaglevThing
    fun draw(built: BuiltMediumData)
}

interface IMaglevPointwiseThing {
    fun transformPoints( lambda: (Vec3f)->(Vec3f))
}

interface IMaglevColorwiseThing {
    // Returns whether or not it makes any changes.
    fun transformColor( lambda: (SColor) -> SColor) :Boolean
}