package spirite.base.brains.palette

import rb.owl.bindable.Bindable
import spirite.base.brains.palette.IPaletteManager.PaletteChangeEvent
import spirite.base.brains.palette.IPaletteManager.PaletteSetChangeEvent
import spirite.base.util.StringUtil
import sguiSwing.hybrid.MDebug
import sguiSwing.hybrid.MDebug.WarningType.STRUCTURAL

abstract class PaletteSet {
    abstract val onPaletteSetChangeTrigger: (PaletteSetChangeEvent) -> Unit
    abstract val onPaletteChangeTrigger: (PaletteChangeEvent) -> Unit

    val palettes : List<Palette> get() = _palettes
    private val _palettes = mutableListOf<Palette>(PSPalette("Default"))

    val currentPaletteBind = Bindable<Palette?>(_palettes.first())
    var currentPalette
        get() = currentPaletteBind.field
        set(value) {
            when {
                value == null || _palettes.contains(value) -> currentPaletteBind.field = value
                else -> TODO("Tried to select palette outside of tis set.  Add proper error handling.")
            }
        }

    fun resetPalettes( newPalette: Collection<Palette>) {
        _palettes.clear()
        _palettes.addAll( newPalette)
        onPaletteSetChangeTrigger(PaletteSetChangeEvent(this))
    }

    fun addPalette( name: String, select: Boolean, raw: ByteArray? = null) : Palette {
        val nondupeName = StringUtil.getNonDuplicateName(_palettes.map { it.name }, name)
        val palette = PSPalette(nondupeName, raw)
        _palettes.add(palette)

        onPaletteSetChangeTrigger(PaletteSetChangeEvent(this))

        if( select)
            currentPalette = palette

        return palette
    }

    fun removePalette( index: Int) {
        if( index < 0 || index >= _palettes.size) {
            MDebug.handleWarning(STRUCTURAL, "Attempt to remove Palette Out of Bounds ($index not in [0,${_palettes.size})")
            return
        }

        val removed = _palettes.removeAt(index)
        if( currentPalette == removed) {
            currentPalette = when {
                index == 0 -> _palettes.firstOrNull()
                else -> _palettes.getOrNull(index-1)
            }
        }
        if( _palettes.size == 0) {
            val defPalette = PSPalette("Default")
            _palettes.add(defPalette )
            currentPalette = defPalette
        }
        onPaletteSetChangeTrigger(PaletteSetChangeEvent(this))
    }

    inner class PSPalette( string: String, raw: ByteArray? = null) : Palette(string, raw) {
        override var onChangeTrigger: (Palette) -> Unit = { onPaletteChangeTrigger.invoke(PaletteChangeEvent(it))}
    }
}