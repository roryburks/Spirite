package spirite.gui.components.basic

import spirite.base.util.delegates.OnChangeDelegate
import spirite.base.brains.Bindable
import spirite.base.util.MathUtil
import spirite.gui.components.advanced.crossContainer.CrossInitializer
import spirite.gui.components.basic.IBoxList.DefaultBoxComponent
import spirite.gui.components.basic.IBoxList.IBoxComponent
import spirite.hybrid.Hybrid
import kotlin.math.max

interface IBoxList<T> : IComponent
{
    val entries : List<T>

    val selectedIndexBind : Bindable<Int>
    var selectedIndex: Int
    var selected : T?

    val numPerRow: Int
    var renderer : (T) -> IBoxComponent

    var movable: Boolean

    fun attemptMove( from: Int, to: Int) : Boolean
    fun addEntry( t: T)
    fun addEntry( ind: Int, t: T)
    fun addAll( set: Collection<T>)
    fun remove( t: T)
    fun clear()

    interface IBoxComponent {
        val component: IComponent
        fun setSelected( selected: Boolean)
        fun setIndex( index: Int) {}
    }

    class DefaultBoxComponent(string: String) : IBoxComponent {
        override val component = Hybrid.ui.Label(string)
        override fun setSelected(selected: Boolean) {}
        override fun setIndex(index: Int) {}
    }
}

abstract class BoxList<T> constructor(boxWidth: Int, boxHeight: Int, entries: Collection<T>?, val del: IBoxListImp)
    :IBoxList<T>, IComponent by del.component
{
    var boxWidth by OnChangeDelegate(boxWidth, {rebuild()})
    var boxHeight by OnChangeDelegate(boxHeight, {rebuild()})

    override val selectedIndexBind = Bindable(0, { new, old ->
        _entries.getOrNull(old)?.component?.setSelected(false)
        _entries.getOrNull(new)?.component?.setSelected(true)
    })
    override var selectedIndex: Int
        get() = selectedIndexBind.field
        set(value) {
            selectedIndexBind.field = MathUtil.clip(-1,value, _entries.size-1)
        }

    override var selected: T? get() = _entries.getOrNull(selectedIndex)?.value
        set(value) {
            selectedIndex = _entries.indexOfFirst { it.value == value }
        }

    override val entries: List<T> get() = _entries.map { it.value }

    protected class BoxListEntry<T>(val value: T, var component: IBoxComponent)
    protected val _entries = mutableListOf<BoxListEntry<T>>()

    override var numPerRow: Int = 0 ; protected set

    override var renderer: (T) -> IBoxComponent = {DefaultBoxComponent(it.toString())}
        set(value) {
            field = value
            _entries.forEachIndexed { index, it ->
                it.component = value.invoke(it.value).apply {
                    setIndex(index)
                    setSelected(selectedIndex == index)
                }
            }
        }

    override var movable: Boolean = true
    override fun attemptMove(from: Int, to: Int) = when(movable) {
        true -> {
            _entries.add(to, _entries.removeAt(from))
            rebuild()
            true
        }
        false -> false
    }

    init {
        entries?.mapIndexed { ind,it->make(it,ind) }?.apply { _entries.addAll(this) }
    }

    override fun addEntry(t: T) {
        _entries.add(make(t, _entries.size))
        rebuild()
    }
    override fun addEntry( ind: Int, t: T) {
        _entries.add(ind, make(t,ind))
        rebuild()
    }
    override fun addAll( set: Collection<T>) {
        _entries.addAll(set.mapIndexed { index, t ->  make(t, _entries.size + index) })
        rebuild()
    }
    override fun remove( t: T) {
        _entries.removeIf { it.value == t }
        rebuild()
    }
    override fun clear() {
        _entries.clear()
        rebuild()
    }

    protected fun make( t: T, ind: Int) = BoxListEntry( t,renderer.invoke(t).apply {
            setSelected(selectedIndex == ind)
            setIndex( ind)
        })

    protected fun rebuild(){
        with( del, {
            val w = width

            numPerRow = max( 1, w/boxWidth)
            val actualWidthPer = w / numPerRow

            del.setLayout {
                for( r in 0..(entries.size/numPerRow)) {
                    rows += {
                        for( c in 0 until numPerRow) {
                            val entry = _entries.getOrNull(r*numPerRow + c)
                            when( entry) {
                                null -> addGap(actualWidthPer)
                                else -> add(entry.component.component, width = actualWidthPer)
                            }
                        }
                        height = boxHeight
                    }
                }
            }
        })
    }

    interface IBoxListImp {
        val component: IComponent
        fun setLayout( constructor: CrossInitializer.()->Unit)
    }
}

