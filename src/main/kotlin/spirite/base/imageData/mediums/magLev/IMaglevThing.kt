package spirite.base.imageData.mediums.magLev

import rb.glow.color.SColor
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
    fun transformColor( lambda: (SColor) -> SColor)
}