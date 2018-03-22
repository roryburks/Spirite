package spirite.gui.components.basic

import spirite.base.util.MUtil
import spirite.base.util.delegates.OnChangeDelegate
import spirite.gui.Bindable
import spirite.gui.components.basic.IBoxList.DefaultBoxComponent
import spirite.gui.components.basic.IBoxList.IBoxComponent
import spirite.hybrid.Hybrid
import spirite.pc.gui.SwUtil
import spirite.pc.gui.basic.*
import java.awt.event.*
import javax.swing.Action
import javax.swing.KeyStroke
import kotlin.math.max
import kotlin.math.min

interface IBoxList<T> : IComponent
{
    val entries : List<T>

    val selectedIndexBind : Bindable<Int>
    var selectedIndex: Int

    val numPerRow: Int
    var renderer : (T) -> IBoxComponent

    var movable: Boolean

    fun attemptMove( from: Int, to: Int) : Boolean

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

class SwBoxList<T>
private constructor(boxWidth: Int, boxHeight: Int, entries: Collection<T>?, val imp: SwBoxListImp<T>)
    :IBoxList<T>, ISwComponent by SwComponent(imp)
{
    constructor(boxWidth:Int, boxHeight: Int, entries: Collection<T>? = null) : this(boxWidth, boxHeight, entries, SwBoxListImp())

    var boxWidth by OnChangeDelegate(boxWidth, {rebuild()})
    var boxHeight by OnChangeDelegate(boxHeight, {rebuild()})

    override val selectedIndexBind = Bindable(0, {new, old ->
        _entries.getOrNull(old)?.component?.setSelected(false)
        _entries.getOrNull(new)?.component?.setSelected(true)
    })
    override var selectedIndex: Int
        get() = selectedIndexBind.field
        set(value) {
            selectedIndexBind.field = MUtil.clip(-1,value, _entries.size-1)
        }

    override val entries: List<T> get() = _entries.map { it.value }

    private class BoxListEntry<T>(val value: T, var component: IBoxComponent) {
    }
    private val _entries = mutableListOf<BoxListEntry<T>>()

    override var numPerRow: Int = 0 ; private set

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
            true
        }
        false -> false
    }

    init {
        entries?.map { BoxListEntry(it, renderer.invoke(it)) }?.apply { _entries.addAll(this) }

        imp.addComponentListener( object : ComponentAdapter(){
            override fun componentResized(e: ComponentEvent?) {rebuild()}
        })
        imp.content.addMouseListener(object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                imp.requestFocus()
                val comp = imp.content.getComponentAt(e.point)
                val index = _entries.indexOfFirst { it.component.component.jcomponent == comp }
                selectedIndex = index
            }
        })
    }
    init /*Map*/ {
        val actionMap = HashMap<KeyStroke,Action>(4)

        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), SwUtil.buildAction{
            if (selectedIndex != -1)
                selectedIndex = max(0, selectedIndex - numPerRow)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), SwUtil.buildAction{
            if (selectedIndex != -1)
                selectedIndex = min(_entries.size - 1, selectedIndex + numPerRow)
        })
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), SwUtil.buildAction({ e ->
            if (selectedIndex != -1)
                selectedIndex = max(0, selectedIndex - 1)
        }))
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), SwUtil.buildAction({ e ->
            if (selectedIndex != -1)
                selectedIndex = min(_entries.size - 1, selectedIndex + 1)
        }))
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction({ e ->
            if (selectedIndex != -1 && selectedIndex != 0) {
                if (attemptMove(selectedIndex, selectedIndex - 1))
                    selectedIndex -= 1
            }
        }))
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction({ e ->
            if (selectedIndex != -1 && selectedIndex != _entries.size - 1) {
                if (attemptMove(selectedIndex, selectedIndex + 1))
                    selectedIndex += 1
            }
        }))
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction({ e ->
            val to = Math.max(0, selectedIndex - numPerRow)

            if (selectedIndex != -1 && to != selectedIndex) {
                if (attemptMove(selectedIndex, to))
                    selectedIndex = to
            }
        }))
        actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK), SwUtil.buildAction{
            val to = Math.min(_entries.size - 1, selectedIndex + numPerRow)

            if (selectedIndex != -1 && to != selectedIndex) {
                if (attemptMove(selectedIndex, to))
                    selectedIndex = to
            }
        })

        SwUtil.buildActionMap(imp, actionMap)
    }

    private fun rebuild(){
        with( imp, {
            content.removeAll()
            val w = width

            numPerRow = max( 1, w/boxWidth)
            val actualWidthPer = w / numPerRow
            println(actualWidthPer)

            layout = CrossLayout.buildCrossLayout(this, {

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
            })
        })
    }

    private class SwBoxListImp<T>()  : SJPanel()
    {
        val content get() = this//SJPanel()
        //val scroll = SScrollPane(content)

        init {
            //layout = GridLayout()
            //add(content)
        }
    }
}