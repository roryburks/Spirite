package spirite.base.brains.palette

import rb.owl.bindable.Bindable
import rb.vectrix.mathUtil.MathUtil
import spirite.base.util.Color
import spirite.base.util.Colors

class PaletteBelt
{
    private val activeColorBinds = mutableListOf(
            Bindable<Color>(Colors.BLACK),
            Bindable<Color>(Colors.WHITE),
            Bindable<Color>(Colors.RED),
            Bindable<Color>(Colors.BLACK))

    fun getColorBind(i: Int): Bindable<Color> = activeColorBinds[i]

    fun getColor(i: Int): Color = activeColorBinds[MathUtil.cycle(0, activeColorBinds.size, i)].field

    fun setColor(i: Int, color: Color) {
        activeColorBinds[MathUtil.cycle(0, activeColorBinds.size, i)].field = color
    }

    fun cycleColors(amount: Int) {
        val new = (0 until activeColorBinds.size).map { activeColorBinds[MathUtil.cycle(0, activeColorBinds.size, it + amount)].field }
        (0 until activeColorBinds.size).forEach {activeColorBinds[it].field = new[it]}
    }
}