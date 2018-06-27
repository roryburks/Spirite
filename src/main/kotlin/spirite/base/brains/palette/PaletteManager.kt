package spirite.base.brains.palette

import spirite.base.brains.Bindable
import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.brains.palette.IPaletteManager.MPaletteObserver
import spirite.base.util.Color
import spirite.base.util.Colors
import spirite.base.util.MathUtil

interface IPaletteManager {
    fun getColorBind( i: Int) : Bindable<Color>
    fun getActiveColor( i : Int) : Color
    fun setActiveColor( i: Int, color: Color)
    fun cycleActiveColors( amount: Int)

    fun makePaletteSet() : PaletteSet

    val currentPaletteBind : Bindable<Palette>
    val currentPalette: Palette

    interface MPaletteObserver {
        fun colorChanged()
    }
    val paletteObservable : IObservable<MPaletteObserver>
}

class PaletteManager : IPaletteManager {
//    val activeColors = mutableListOf<Color>(Colors.BLACK, Colors.WHITE, Colors.RED, Colors.BLACK)

    val activeColorBinds = mutableListOf(
            Bindable<Color>(Colors.BLACK),
            Bindable<Color>(Colors.WHITE),
            Bindable<Color>(Colors.RED),
            Bindable<Color>(Colors.BLACK))

    override fun getColorBind(i: Int): Bindable<Color> = activeColorBinds[i]

    override fun getActiveColor(i: Int): Color = activeColorBinds[ MathUtil.cycle(0, activeColorBinds.size, i)].field

    override fun setActiveColor(i: Int, color: Color) {
        activeColorBinds[ MathUtil.cycle(0, activeColorBinds.size, i)].field = color
    }

    override fun cycleActiveColors(amount: Int) {
        val new = (0 until activeColorBinds.size).map { activeColorBinds[MathUtil.cycle(0,activeColorBinds.size,it + amount)].field }
        (0 until activeColorBinds.size).forEach {activeColorBinds[it].field = new[it]}
    }

    override fun makePaletteSet(): PaletteSet = PMPaletteSet()
    private inner class PMPaletteSet : PaletteSet() {
        override val onChangeTrigger: (PaletteSet) -> Unit = {triggerPaletteChange()}
    }

    override val paletteObservable: IObservable<MPaletteObserver> get() = _paletteObs
    private val _paletteObs = Observable<MPaletteObserver>()

    private fun triggerPaletteChange() {
        _paletteObs.trigger { obs -> obs.colorChanged() }
    }

    val globalPaletteSet : PaletteSet = PMPaletteSet()

    override val currentPaletteBind = Bindable(globalPaletteSet.currentPalette)
    override val currentPalette: Palette by currentPaletteBind
}

