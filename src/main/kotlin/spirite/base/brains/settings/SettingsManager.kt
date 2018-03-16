package spirite.base.brains.settings

import spirite.base.util.delegates.MutableLazy
import spirite.base.util.interpolation.CubicSplineInterpolator
import spirite.base.util.linear.Vec2
import java.io.File
import java.nio.ByteBuffer
import kotlin.reflect.KProperty

interface ISettingsManager {

}

class SettingsManager (
        private val preferences: IPreferences
): ISettingsManager{
    // ================
    // ==== Graphics Engine Management

    // TODO


    // ==============
    // ==== Palettes:
    // region Palettes

    val paletteList : List<String> get() = _paletteList
    private val _paletteList: MutableList<String> by lazy {
        (preferences.getString("PaletteList") ?: "")
                .split("${0.toChar()}")
                .toMutableList()
    }

    fun getRawPalette( name: String) = preferences.getByteArray( "palette.$name")
    fun saveRawPalette( name: String, raw: ByteArray) {
        if( !_paletteList.contains(name)) {
            _paletteList.add( name)
            preferences.putString("PaletteList", _paletteList.joinToString("${0.toChar()}"))
        }

        preferences.putByteArray("palette.$name", raw)
    }

    // endregion

    // ====================
    // ==== Recent-Used FilePath
    // region Recent-Used File Paths

    // TODO: Make less dependent on Java System stuff
    var lastUsedWorkspace = true

    var workspaceFilePath : File
        get() = File(_workspaceFilePath)
        set(value) {
            _workspaceFilePath = value.path
            lastUsedWorkspace = true
        }
    private var _workspaceFilePath by PreferenceStringDelegate("wsPath", System.getProperty("user.dir"))

    var imageFilePath : File
        get() = File(_imageFilePath)
        set(value) {
            _imageFilePath = value.path
            lastUsedWorkspace = false
        }
    private var _imageFilePath by PreferenceStringDelegate( "imgPath", System.getProperty("user.dir"))

    var aafFilePath : File
        get() = File(_aafFilePath)
        set(value) { _aafFilePath = value.path}
    private var _aafFilePath : String by PreferenceStringDelegate("aafPath", System.getProperty("user.dir"))

    val openFilePath get() = if( lastUsedWorkspace) workspaceFilePath else imageFilePath

    // endregion

    // ===============
    // ==== Simple Settings
    // region Simple Settings
    var defaultImageWidth: Int by PreferenceIntDelegate( "defaultImageWidth", 640)
    var defaultImageHeight: Int by PreferenceIntDelegate( "defaultImageHeight", 480)

    var promptOnGroupCrop : Boolean by PreferenceBooleanDelegate( "promptOnGroupCrop", true)
    var DEBUG : Boolean by PreferenceBooleanDelegate("DEBUG", false)
    var allowsEdittingInvisible by PreferenceBooleanDelegate("InvisEditing", false)

    var thumbnailSize by PreferenceIntDelegate("thumbnailSize", 32)

    var scrollBuffer by PreferenceIntDelegate("scrollBuffer", 100)
    // endregion


    // ============
    // ==== Tablet Pressure Curve
    // region Tablet Pressure Curve

    val tabletPressureInterpolator get() = _tabletPressureInterpolator
    var _tabletPressureInterpolator : CubicSplineInterpolator by MutableLazy { cubicSplineInterpolatorFromByteArray(preferences.getByteArray("tcpPoints")) }

    /** Changes the intepolator for interpetting tablet pressure to one constructed
     * from the given points, saving it to preferences as it makes it.
     *
     * @param points list of points, must be non-null, at least 1 big, each point should
     * be in between (0,0) and (1,1), inclusive
     * @return the constructed Interpolator
     * */
    fun setTabletInterpolationPoints( points: List<Vec2>) : CubicSplineInterpolator {
        // TODO: Error Checking

        val bb = ByteBuffer.allocate( 2 + 8*2*points.size)

        bb.putShort( points.size.toShort())
        points.forEach {
            bb.putFloat( it.x)
            bb.putFloat( it.y)
        }
        preferences.putByteArray("tpcPoints", bb.array())

        _tabletPressureInterpolator = CubicSplineInterpolator( points, true, true)
        return _tabletPressureInterpolator
    }

    private fun cubicSplineInterpolatorFromByteArray( raw: ByteArray?) : CubicSplineInterpolator {
        val raw = raw ?: byteArrayOf(0, 0, 1, 1)

        val buff = ByteBuffer.wrap( raw)
        val num = buff.getShort()

        val points = List( num.toInt(), {
            val x = buff.getFloat()
            val y = buff.getFloat()
            Vec2( x, y)
        })

        return CubicSplineInterpolator( points, true, true)
    }
    // endregion

    // =============
    // ==== Delegates
    // region Delegates

    private inner class PreferenceStringDelegate( val key: String, val default: String) {
        var field : String? = null

        operator fun getValue(thisRef: SettingsManager, prop: KProperty<*>): String {
            val ret = field ?: preferences.getString(key) ?: default
            field = ret
            return ret
        }

        operator fun setValue(thisRef:SettingsManager, prop: KProperty<*>, value: String) {
            field = value
            preferences.putString(key, value)
        }
    }
    private inner class PreferenceIntDelegate( val key: String, val default: Int) {
        var field : Int? = null

        operator fun getValue(thisRef: SettingsManager, prop: KProperty<*>): Int {
            val ret = field ?: preferences.getInt(key, default)
            field = ret
            return ret
        }

        operator fun setValue(thisRef:SettingsManager, prop: KProperty<*>, value: Int) {
            field = value
            preferences.putInt(key, value)
        }
    }
    private inner class PreferenceBooleanDelegate( val key: String, val default: Boolean) {
        var field : Boolean? = null

        operator fun getValue(thisRef: SettingsManager, prop: KProperty<*>): Boolean {
            val ret = field ?: preferences.getBoolean(key, default)
            field = ret
            return ret
        }

        operator fun setValue(thisRef:SettingsManager, prop: KProperty<*>, value: Boolean) {
            field = value
            preferences.putBoolean(key, value)
        }
    }

    // endregion
}
