package spirite.base.pen.stroke

import spirite.base.pen.PenState

interface PenDynamics {
    fun getSize( ps: PenState) : Float
}

object BasicDynamics : PenDynamics {
    override fun getSize(ps: PenState) = ps.pressure
}