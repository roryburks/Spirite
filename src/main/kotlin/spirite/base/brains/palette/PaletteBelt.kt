package spirite.base.brains.palette

import spirite.base.util.binding.CruddyBindable
import spirite.base.util.Color
import spirite.base.util.Colors
import rb.vectrix.mathUtil.MathUtil

class PaletteBelt
{
    private val activeColorBinds = mutableListOf(
            CruddyBindable<Color>(Colors.BLACK),
            CruddyBindable<Color>(Colors.WHITE),
            CruddyBindable<Color>(Colors.RED),
            CruddyBindable<Color>(Colors.BLACK))

    fun getColorBind(i: Int): CruddyBindable<Color> = activeColorBinds[i]

    fun getColor(i: Int): Color = activeColorBinds[MathUtil.cycle(0, activeColorBinds.size, i)].field

    fun setColor(i: Int, color: Color) {
        activeColorBinds[MathUtil.cycle(0, activeColorBinds.size, i)].field = color
    }

    fun cycleColors(amount: Int) {
        val new = (0 until activeColorBinds.size).map { activeColorBinds[MathUtil.cycle(0, activeColorBinds.size, it + amount)].field }
        (0 until activeColorBinds.size).forEach {activeColorBinds[it].field = new[it]}
    }
}