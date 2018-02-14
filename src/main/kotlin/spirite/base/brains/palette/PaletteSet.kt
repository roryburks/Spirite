package spirite.base.brains.palette

import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.STRUCTURAL

abstract class PaletteSet {
    abstract val onChangeTrigger: (PaletteSet) -> Unit

    val palettes : List<Palette> get() = _palettes
    private val _palettes = mutableListOf<Palette>()

    var selectedPalette : Int
        get() = _selectedPalette
        set(value) {
            _selectedPalette = value
            onChangeTrigger.invoke(this)
        }
    private var _selectedPalette = 0

    val currentPalette : Palette get() =  _palettes[_selectedPalette]

    fun resetPalettes( newPalette: Collection<Palette>) {
        _palettes.clear()
        _palettes.addAll( newPalette)
        onChangeTrigger.invoke(this)
    }

    fun addPalette( name: String, select: Boolean, raw: ByteArray? = null) : Palette {
        val palette = PSPalette(name, raw)
        _palettes.add(palette)
        if( select)
            _selectedPalette = _palettes.size - 1
        onChangeTrigger.invoke(this)

        return palette
    }
//
//    fun addPalette(palette: Palette, select: Boolean) {
//        _palettes.add( palette)
//        if(select)
//            _selectedPalette = _palettes.size - 1
//        onChangeTrigger.invoke(this)
//    }

    fun removePalette( index: Int) {
        if( index < 0 || index >= _palettes.size) {
            MDebug.handleWarning(STRUCTURAL, "Attempt to remove Palette Out of Bounds ($index not in [0,${_palettes.size})")
            return
        }

        _palettes.removeAt(index)
        if( _selectedPalette >= index)
            _selectedPalette--
        if( _palettes.size == 0) {
            _palettes.add( PSPalette("Default"))
            _selectedPalette = 0
        }
        onChangeTrigger.invoke(this)
    }

    inner class PSPalette( string: String, raw: ByteArray? = null) : Palette(string, raw) {
        override var onChangeTrigger: (Palette) -> Unit = { this@PaletteSet.onChangeTrigger.invoke(this@PaletteSet)}
    }
}