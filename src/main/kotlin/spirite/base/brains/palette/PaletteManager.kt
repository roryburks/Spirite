package spirite.base.brains.palette

import spirite.base.brains.*
import spirite.base.brains.palette.IPaletteManager.*

interface IPaletteManager {
    val activeBelt : PaletteBelt

    fun makePaletteSet() : PaletteSet

    val currentPaletteBind : IBindable<Palette>
    val currentPalette: Palette
    val globalPalette: Palette

    interface PaletteObserver {
        fun paletteChanged( evt: PaletteChangeEvent)
        fun paletteSetChanged( evt: PaletteSetChangeEvent)
    }
    data class PaletteChangeEvent(val palette: Palette)
    data class PaletteSetChangeEvent(val paletteSet: PaletteSet)

    val paletteObservable : IObservable<PaletteObserver>
}

class PaletteManager(private val workspaceSet: IWorkspaceSet) : IPaletteManager {
    override val activeBelt: PaletteBelt = PaletteBelt()

    override fun makePaletteSet(): PaletteSet {
        val newPaletteSet = object : PaletteSet() {
            override val onPaletteSetChangeTrigger: (PaletteSetChangeEvent) -> Unit = { triggerPaletteSetChange(it) }
            override val onPaletteChangeTrigger: (PaletteChangeEvent) -> Unit = { triggerPaletteChange(it) }
        }

        // DuckTape way of getting PaletteManager.currentPalette to track CurrentWorkspace.PaletteSet.CurrentPalette
        newPaletteSet.currentPaletteBind.addListener { new, old ->
            if( workspaceSet.currentWorkspace?.paletteSet == newPaletteSet)
                currentPaletteBind.field = new ?: globalPalette
        }

        return newPaletteSet
    }

    override val paletteObservable = Observable<PaletteObserver>()

    override val globalPalette = object : Palette("Global") {
        override val onChangeTrigger: (Palette) -> Unit = {triggerPaletteChange(PaletteChangeEvent(this))}
    }

    override val currentPaletteBind = Bindable(globalPalette)
    override val currentPalette: Palette by currentPaletteBind

    private val wsObs = workspaceSet.currentWorkspaceBind.addListener { new, _ ->
        currentPaletteBind.field = new?.paletteSet?.currentPalette ?: globalPalette
    }


    private fun triggerPaletteChange(evt: PaletteChangeEvent)
            = paletteObservable.trigger { obs -> obs.paletteChanged(evt) }
    private fun triggerPaletteSetChange(evt: PaletteSetChangeEvent)
            = paletteObservable.trigger { obs -> obs.paletteSetChanged(evt) }
}

