package spirite.gui.advanced

import spirite.gui.Orientation
import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.basic.IComponent
import javax.swing.GroupLayout
import javax.swing.GroupLayout.ParallelGroup
import javax.swing.GroupLayout.SequentialGroup
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.Int.Companion

open class CrossContainer(constructor: CrossInitializer.()->Unit)
    : JPanel(), IComponent
{
    val rootOrientation: Orientation
    internal val rootGroup : CSE_Group?

    init {
        val initObj = CrossInitializer()
        constructor.invoke(initObj)

        rootOrientation = initObj.orientation
        rootGroup = initObj.rootGroup
        initLayout()
    }

    fun initLayout() {
        val hor = (rootOrientation == HORIZONATAL)

        val layout = GroupLayout( this)
        this.layout = layout

        if( rootGroup != null) {
            val pGroup = layout.createParallelGroup()
            val sGroup = layout.createSequentialGroup()

            fun rec( group : CSE_Group, pGroup: ParallelGroup, sGroup: GroupLayout.Group) {
                group.subComponents.forEach {
                    when( it) {
                        is CSE_Gap -> {
                            sGroup.addGap(it.minWidth, it.defaultWidth, it.maxWidth)
                        }
                        is CSE_Component -> {
                            val comp = it.component as JComponent
                            when {
                                it.fixed != null -> sGroup.addComponent(comp, it.fixed, it.fixed, it.fixed)
                                it.flex != null -> sGroup.addComponent(comp, 0, GroupLayout.DEFAULT_SIZE, Int.MAX_VALUE)
                                else -> sGroup.addComponent(comp)
                            }

                            when {
                                it.overrideGroup != null -> pGroup.addComponent(it.component as JComponent, it.overrideGroup, it.overrideGroup, it.overrideGroup)
                                group.fixed != null ->  pGroup.addComponent(it.component as JComponent, group.fixed, group.fixed, group.fixed)
                                else ->  pGroup.addComponent(it.component as JComponent, 0, (100 * (group.flex?:1f)).toInt(), Int.MAX_VALUE)
                            }
                        }
                        is CSE_Group -> {
                            val npGroup = layout.createParallelGroup()
                            val nsGroup = layout.createSequentialGroup()
                            pGroup.addGroup(nsGroup)
                            sGroup.addGroup(npGroup)
                            rec( it, npGroup, nsGroup)
                        }
                    }
                }
            }

            rec( rootGroup, pGroup, sGroup)

            layout.setVerticalGroup(sGroup)
            layout.setHorizontalGroup(pGroup)
        }

    }
}

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
    internal val orientation = when {
        _cols != null -> Orientation.HORIZONATAL
        else -> VERTICAL
    }

    internal val rootGroup get() = when {
        _cols != null -> CSE_Group(_cols!!.entities, _cols!!.height, _cols!!.flex, false)
        _rows != null -> CSE_Group(_rows!!.entities, _rows!!.width, _rows!!.flex, false)
        else -> null
    }
}

sealed class CrossSequenceInitializer {
    internal val entities = mutableListOf<CrossSequenceEntity>()

    fun addGap( size: Int) {
        entities.add(CSE_Gap(size,size,size))
    }
    fun addGap( minWidth: Int, defaultWidth: Int, maxWidth: Int) {
        entities.add(CSE_Gap(minWidth, defaultWidth, maxWidth))
    }

    var flex: Float? = null
}

class CrossRowInitializer : CrossSequenceInitializer() {
    var height: Int? = null

    fun add( component: IComponent, width : Int? = null, height: Int? = null, flex: Float? = null) {
        entities.add(CSE_Component(component, height, width, flex))
    }

    operator fun plusAssign(constructor: CrossRowInitializer.() -> Unit) {
        val initObj  = CrossRowInitializer()
        constructor.invoke(initObj)
        entities.add( CSE_Group(initObj.entities, initObj.height, initObj.flex, false))
    }
    fun addFlatFroup(constructor: CrossRowInitializer.() -> Unit) {
        val initObj  = CrossRowInitializer()
        constructor.invoke(initObj)
        entities.add( CSE_Group(initObj.entities, initObj.height, initObj.flex, true))
    }
}
class CrossColInitializer : CrossSequenceInitializer() {
    var width : Int? = null

    fun add( component: IComponent, width : Int? = null, height: Int? = null, flex: Float? = null) {
        entities.add(CSE_Component(component, width, height, flex))
    }

    operator fun plusAssign(constructor: CrossRowInitializer.() -> Unit) {
        val initObj  = CrossRowInitializer()
        constructor.invoke(initObj)
        entities.add( CSE_Group(initObj.entities, initObj.height, initObj.flex, false))
    }
    fun addFlatFroup(constructor: CrossRowInitializer.() -> Unit) {
        val initObj  = CrossRowInitializer()
        constructor.invoke(initObj)
        entities.add( CSE_Group(initObj.entities, initObj.height, initObj.flex, true))
    }
}

sealed class CrossSequenceEntity

class CSE_Gap(
        val minWidth: Int,
        val defaultWidth: Int,
        val maxWidth: Int) : CrossSequenceEntity()

class CSE_Component(
        val component: IComponent,
        val overrideGroup : Int?,
        val fixed: Int?,
        val flex: Float?) : CrossSequenceEntity()

class CSE_Group(
        val subComponents: List<CrossSequenceEntity>,
        val fixed: Int?,
        val flex: Float?,
        val flat: Boolean) : CrossSequenceEntity()