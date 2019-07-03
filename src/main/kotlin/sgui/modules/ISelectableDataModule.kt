package sgui.modules

import rb.extendo.delegates.OnChangeDelegate
import rb.extendo.extensions.then
import rb.owl.GuardedObservable
import rb.owl.IObservable
import rb.owl.Observer
import rb.owl.bindable.Bindable

interface ISelectableDataModule<T> where T:Any
{

    val selectionObs : IObservable<() -> Unit>
    var multiSelectEnabled: Boolean
    val currentSelectedSet: List<T>
    val selectedIndexBind : Bindable<Int>
    var selectedIndex: Int
    var selected : T?

    fun setSelection(t: T?)
    fun addSelection(t: T)
    fun removeSelection(t: T)
    fun setSelection(selectionSet: Set<T>, primarySelected: T? = null)

    val entryObs: IObservable<() -> Unit>
    val entries : List<T>
    fun add( t: T)
    fun add( t: Collection<T>)
    fun remove( t: T)
    fun clear()
    fun resetAllWithSelection(set: Iterable<T>, selected: Set<T>, primarySelected: T?)
}

class SelectableDataModule<T>(
        startingEntries: List<T>,
        multiSelectStartsEnabled : Boolean = false)
    : ISelectableDataModule<T> where T:Any
{
    private val _selectedSet = mutableSetOf<Int>()
    private val _entries = startingEntries.toMutableList()

    override val selectionObs = GuardedObservable<() -> Unit>()
    override var multiSelectEnabled: Boolean by OnChangeDelegate(multiSelectStartsEnabled) {
        if( !it)
            reconcileSelectionSet(_selectedSet.take(1))
    }

    override val currentSelectedSet: List<T> get() = _selectedSet
            .mapNotNull { _entries.getOrNull(it) }
    override val selectedIndexBind = Bindable(if( startingEntries.any()) 0 else -1)
            { if( it  in -1 until _entries.size) it else -1}
    override var selectedIndex: Int by selectedIndexBind
    private val _selectedIndexK = selectedIndexBind.addObserver(Observer{ new, _->
        val set = when {
            multiSelectEnabled && new in 0 until entries.size -> _selectedSet.then(new)
            multiSelectEnabled -> _selectedSet
            new in 0 until entries.size ->  setOf(new)
            else -> emptySet()
        }
        reconcileSelectionSet(set, new)
    }, false)

    override var selected: T?
        get() = _entries.getOrNull(selectedIndex)
        set(value) {selectedIndex = _entries.indexOf(value)}

    override fun setSelection(t: T?) {
        val ti = if( t == null) -1 else entries.indexOf(t)
        reconcileSelectionSet(if( ti in 0 until entries.size) setOf(ti) else emptySet(), ti)
    }

    override fun addSelection(t: T) {
        if( !_entries.contains(t))
            return

        val ti = _entries.indexOf(t)

        if( multiSelectEnabled)
            reconcileSelectionSet(_selectedSet.then(ti))
        else
            reconcileSelectionSet(listOf(ti), ti)
    }

    override fun removeSelection(t: T) {
        val ti = _entries.indexOf(t)
        reconcileSelectionSet(_selectedSet.filter { it != ti })
    }

    override fun setSelection(selectionSet: Set<T>, primarySelected: T?) {
        val newSelectionSet = selectionSet
                .map { _entries.indexOf(it) }
                .filter { it != -1 }
        val toSelectIndex = if( primarySelected == null) -1 else _entries.indexOf(primarySelected)

        reconcileSelectionSet(newSelectionSet, toSelectIndex)
    }

    override val entryObs = GuardedObservable<() -> Unit>()
    override val entries: List<T> get() = _entries

    override fun add(t: T) {
        _entries.add(t)
        reconcileSelectionSet()
        entryObs.trigger { it() }
    }

    override fun add(t: Collection<T>) {
        _entries.addAll(t)
        reconcileSelectionSet()
        entryObs.trigger { it() }
    }

    override fun remove(t: T) {
        _entries.remove(t)
        reconcileSelectionSet()
        entryObs.trigger { it() }
    }

    override fun clear() {
        _entries.clear()
        reconcileSelectionSet()
        entryObs.trigger { it() }
    }

    override fun resetAllWithSelection(set: Iterable<T>, selected: Set<T>, primarySelected: T?) {
        _entries.clear()
        _entries.addAll(set)

        setSelection(selected, primarySelected)
        entryObs.trigger { it() }
    }

    private var _recursiveBlock = false
    private fun reconcileSelectionSet(selectionSet: Iterable<Int> = _selectedSet, toSelect: Int = selectedIndex)
    {
        if(!_recursiveBlock)
        {
            try {
                _recursiveBlock = true
                val oldSelectionSet = _selectedSet.toSet()
                val newSelectionSet = selectionSet
                        .filter { it != -1 && it < _entries.size }
                        .run { if( multiSelectEnabled) this else take(1) }  // rare actual necessary use of run or bad design?
                        .toSet()
                val fromSelectedIndex = selectedIndex
                val toSelectedIndex = when {
                    !newSelectionSet.contains(toSelect ) -> -1
                    toSelect  in -1 until _entries.size -> toSelect
                    else -> -1
                }

                _selectedSet.clear()
                _selectedSet.addAll(newSelectionSet)
                selectedIndex = toSelectedIndex

                if( toSelectedIndex != fromSelectedIndex
                        || oldSelectionSet.any{!newSelectionSet.contains(it)}
                        || newSelectionSet.any{!oldSelectionSet.contains(it)})
                {
                    selectionObs.trigger { it() }
                }
            }
            finally {
                _recursiveBlock = false
            }
        }
    }
}