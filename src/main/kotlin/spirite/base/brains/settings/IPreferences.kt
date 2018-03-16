package spirite.base.brains.settings

import java.util.prefs.Preferences

interface IPreferences {
    fun getString( key: String) : String?
    fun getByteArray( key: String) : ByteArray?
    fun getInt( key: String, default: Int) : Int
    fun getBoolean( key: String, default: Boolean) : Boolean

    fun putString( key: String, value: String)
    fun putByteArray( key: String, value: ByteArray)
    fun putInt( key: String, value: Int)
    fun putBoolean( key: String, value: Boolean)

    fun remove( key: String)
}

class JPreferences(
        userNode : Class<*>
) : IPreferences {
    val preferences = Preferences.userNodeForPackage( userNode)

    override fun getString(key: String) = preferences.get(key, null)
    override fun getByteArray(key: String) = preferences.getByteArray(key, null)
    override fun getInt(key: String, default: Int) = preferences.getInt( key, default)
    override fun getBoolean(key: String, default: Boolean) = preferences.getBoolean( key, default)

    override fun putString(key: String, value: String) = preferences.put(key, value)
    override fun putByteArray(key: String, value: ByteArray) = preferences.putByteArray(key, value)
    override fun putInt(key: String, value: Int) = preferences.putInt(key, value)
    override fun putBoolean(key: String, value: Boolean) = preferences.putBoolean(key, value)

    override fun remove(key: String) = preferences.remove(key)
}