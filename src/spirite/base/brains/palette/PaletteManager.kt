package spirite.base.brains.palette

import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.brains.palette.IPaletteManager.MPaletteObserver
import spirite.base.util.Colors
import spirite.base.util.MUtil.cycle

interface IPaletteManager {
    fun getActiveColor( i : Int) : Int
    fun setActiveColor( i: Int, color: Int)
    fun cycleActiveColors( amount: Int)

    fun makePaletteSet() : PaletteSet

    interface MPaletteObserver {
        fun colorChanged()
    }
    val paletteObservable : IObservable<MPaletteObserver>
}

class PaletteManager : IPaletteManager {
    val activeColors = mutableListOf<Int>(Colors.BLACK, Colors.WHITE, Colors.RED, Colors.BLACK)

    override fun getActiveColor(i: Int): Int = activeColors[ cycle(0, activeColors.size, i)]

    override fun setActiveColor(i: Int, color: Int) {
        activeColors[ cycle(0, activeColors.size, i)] = color
    }

    override fun cycleActiveColors(amount: Int) {
        val new = (0 until activeColors.size).map { activeColors[cycle(0,activeColors.size,it + amount)] }
        (0 until activeColors.size).forEach {activeColors[it] = new[it]}
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
}

