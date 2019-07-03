package sgui.components

import rb.extendo.delegates.OnChangeDelegate
import rb.extendo.extensions.removeToList
import rb.owl.Observer
import sgui.components.IBoxList.IBoxComponent
import sgui.components.IBoxList.IMovementContract
import sgui.components.crossContainer.CrossInitializer
import sgui.components.events.MouseEvent
import sgui.modules.ISelectableDataModule
import sgui.modules.SelectableDataModule
import kotlin.math.max

interface IBoxList<T> : IComponent where T : Any
{
    val data: ISelectableDataModule<T>

    var movementContract : IMovementContract?

    val numPerRow: Int
    var renderer : (T) -> IBoxComponent

    var movable: Boolean
    fun attemptMove( from: Int, to: Int) : Boolean

    interface IBoxComponent {
        val component: IComponent
        fun onClick( mouseEvent: MouseEvent) {}
        fun setSelected( selected: Boolean)
        fun setIndex( index: Int) {}
        fun onCleared() {}
    }

    interface IMovementContract
    {
        fun canMove(from: Int, to:Int) : Boolean
        fun doMove(from: Int, to: Int)
    }
}

abstract class BoxList<T> constructor(
        boxWidth: Int,
        boxHeight: Int,
        entries: Collection<T>?,
        private val _provider: IComponentProvider,
        val del: IBoxListImp,
        multiSelect: Boolean = false)
    : IBoxList<T>, IComponent by del.component
        where T: Any
{
    var boxWidth by OnChangeDelegate(boxWidth, { rebuild() })
    var boxHeight by OnChangeDelegate(boxHeight, { rebuild() })


    final override val data = SelectableDataModule<T>(entries?.toList() ?: emptyList(), multiSelect)

    protected val _componentMap = mutableMapOf<T, IBoxComponent>()

    override var numPerRow: Int = 0 ; protected set

    override var renderer by OnChangeDelegate<(T) -> IBoxComponent>({DefaultBoxComponent(it.toString())}) {reconcileUi()}

    private  inner class DefaultBoxComponent(string: String) : IBoxComponent {
        override val component = _provider.Label(string)
        override fun setSelected(selected: Boolean) {}
        override fun setIndex(index: Int) {}
    }

    override var movementContract: IMovementContract? = null
    override var movable: Boolean = true
    override fun attemptMove(from: Int, to: Int) : Boolean{
        if(!movable) return false

        val contract = movementContract
        if( contract != null) {
            if( !contract.canMove(from, to)) return false
            contract.doMove(from, to)
        }

        val partition = data.entries.toMutableList()
        partition.add(to, partition.removeAt(from)) // TODO: Is this logic sound?
        data.resetAllWithSelection(partition, data.currentSelectedSet.toSet(), data.selected)

        return true
    }

    private fun reconcileUi(){
        val selectedComponents = data.currentSelectedSet.toSet()

        val newData = data.entries
        val newDataAsSet = newData.toHashSet()
        _componentMap.entries
                .removeToList{!newDataAsSet.contains(it.key)}
                .forEach { it.value.onCleared() }

        newData.forEachIndexed { index, it ->
            val oldComponent = _componentMap[it]
            val newComponent = renderer.invoke(it)
            if(oldComponent != newComponent){
                oldComponent?.onCleared()
                _componentMap[it] = newComponent
            }
            newComponent.apply {
                setIndex(index)
                setSelected(selectedComponents.contains(it))
            }
        }
    }

    private val _modifyK = data.entryObs.addObserver(Observer{rebuild()})
    private val _selectK = data.selectionObs.addObserver(Observer{rebuild()})

    protected fun rebuild(){
        reconcileUi()
        val w = width

        numPerRow = max( 1, w/boxWidth)
        val actualWidthPer = w / numPerRow

        del.setLayout {
            for( r in 0..(data.entries.size/numPerRow)) {
                rows += {
                    for( c in 0 until numPerRow) {
                        val entry = data.entries.getOrNull(r*numPerRow + c)
                        val component = _componentMap[entry]
                        when( component) {
                            null -> addGap(actualWidthPer)
                            else -> add(component.component, width = actualWidthPer)
                        }
                    }
                    height = boxHeight
                }
            }
        }
    }

    interface IBoxListImp {
        val component: IComponent
        fun setLayout( constructor: CrossInitializer.()->Unit)
    }
}

