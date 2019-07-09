package sgui.components.crossContainer

import sgui.Orientation.HORIZONTAL
import sgui.Orientation.VERTICAL
import sgui.components.IComponent

class CrossInitializer {
    val rows
        get() = when {
            _rows != null -> _rows!!
            _cols != null -> throw Exception("Tried to Initialize both Rows and Cols")
            else -> {
                _rows = CrossColInitializer()
                _rows!!
            }
        }
    private var _rows : CrossColInitializer? = null

    val cols
        get() = when {
            _cols != null -> _cols!!
            _rows != null -> throw Exception("Tried to Initialize both Rows and Cols")
            else -> {
                _cols = CrossRowInitializer()
                _cols!!
            }
        }
    private var _cols : CrossRowInitializer? = null

    /** Since Initialization uses stylistically-appropriate wording for widths and heights, if something is algorithmically
     * constructing its layout it has to know which orientation its going to use before hand, so to help with things
     * that algorithmically construct layouts that could be oriented either vertically or horizontally, this exists
     * so you can write something in one orientation and then have it built in another*/
    var overwriteOrientation : sgui.Orientation? = null

    internal val scheme get() = when {
        _cols != null -> CrossScheme(overwriteOrientation ?: HORIZONTAL,
                CSE_Group(_cols!!.entities, _cols!!.height, _cols!!.flex, false, _cols!!.padding, null))
        _rows != null -> CrossScheme(overwriteOrientation ?: VERTICAL,
                CSE_Group(_rows!!.entities, _rows!!.width, _rows!!.flex, false, _rows!!.padding, null))
        else -> CrossScheme(VERTICAL, null)
    }
}

sealed class CrossSequenceInitializer {
    internal val entities = mutableListOf<CrossSequenceEntity>()

    fun addGap( size: Int) {
        entities.add(CSE_Gap(size, size, size))
    }
    fun addGap( minWidth: Int, defaultWidth: Int, maxWidth: Int) {
        entities.add(CSE_Gap(minWidth, defaultWidth, maxWidth))
    }

    var flex: Float? = null
    var padding: Int? = null
}

class CrossRowInitializer : CrossSequenceInitializer() {
    var height: Int? = null

    fun add(component: IComponent, width : Int? = null, height: Int? = null, flex: Float? = null) {
        entities.add(CSE_Component(component, height, width, flex))
    }

    operator fun plusAssign(constructor: CrossColInitializer.() -> Unit) {
        val initObj  = CrossColInitializer()
        constructor.invoke(initObj)
        entities.add(CSE_Group(initObj.entities, initObj.width, initObj.flex, false, initObj.padding, null))
    }

    fun addFlatGroup(pregap : Int? = null, constructor: CrossColInitializer.() -> Unit) {
        val initObj  = CrossColInitializer()
        constructor.invoke(initObj)
        entities.add(CSE_Group(initObj.entities, initObj.width, initObj.flex, true, initObj.padding, pregap))
    }
}

class CrossColInitializer : CrossSequenceInitializer() {
    var width : Int? = null

    fun add(component: IComponent, width : Int? = null, height: Int? = null, flex: Float? = null) {
        entities.add(CSE_Component(component, width, height, flex))
    }

    operator fun plusAssign(constructor: CrossRowInitializer.() -> Unit) {
        val initObj  = CrossRowInitializer()
        constructor.invoke(initObj)
        entities.add(CSE_Group(initObj.entities, initObj.height, initObj.flex, false, initObj.padding, null))
    }
    fun addFlatGroup(pregap : Int? = null, constructor: CrossRowInitializer.() -> Unit) {
        val initObj  = CrossRowInitializer()
        constructor.invoke(initObj)
        entities.add(CSE_Group(initObj.entities, initObj.height, initObj.flex, true, initObj.padding, pregap))
    }
}