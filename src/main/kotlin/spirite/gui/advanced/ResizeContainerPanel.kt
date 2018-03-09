package spirite.gui.advanced

import spirite.base.util.MUtil
import spirite.base.util.groupExtensions.then
import spirite.gui.advanced.IResizeContainerPanel.IResizeBar
import spirite.gui.basic.IComponent
import spirite.gui.basic.SPanel
import spirite.gui.basic.SToggleButton
import spirite.gui.Bindable
import spirite.gui.Bindable.Bound
import spirite.gui.Orientation
import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.Skin.ResizePanel.BarLineColor
import spirite.gui.basic.IComponent.MouseEvent
import java.awt.Cursor
import java.awt.Graphics
import kotlin.reflect.KProperty

interface IResizeContainerPanel : IComponent
{

    var minStretch : Int
    var orientation : Orientation
    var barSize : Int
    var stretchComponent : IComponent

    fun getPanel(index: Int) : IResizeBar?
    fun addPanel( component : IComponent, minSize: Int, defaultSize: Int, position: Int = 0) : Int
    fun removePanel( index: Int)

    interface IResizeBar {
        var minSize: Int
        var resizeComponent: IComponent
    }
}

class ResizeContainerPanel(
        stretchComponent: IComponent,
        orientation: Orientation,
        private val defaultSize: Int = 100
) : SPanel(), IResizeContainerPanel {

    override var minStretch: Int by LayoutDelegate(0)
    override var orientation by LayoutDelegate(orientation)
    override var barSize by LayoutDelegate(8)
    override var stretchComponent by LayoutDelegate(stretchComponent)

    private val leadingBars = mutableListOf<ResizeBar>()
    private val trailingBars = mutableListOf<ResizeBar>()

    init {
    }


    override fun getPanel( index: Int) : ResizeBar? = when {
        index < 0 && -index <= leadingBars.size -> leadingBars[-index-1]
        index > 0 && index <= trailingBars.size -> trailingBars[index-1]
        else -> null
    }

    override fun addPanel( component: IComponent, minSize: Int, defaultSize: Int, position: Int) : Int{
        val position = if( position == 0) Integer.MAX_VALUE else position;

        val ret = when {
            position < 0 && -position >= trailingBars.size -> {
                trailingBars.add(ResizeBar(defaultSize, minSize, component, true))
                -trailingBars.size
            }
            position < 0 -> {
                trailingBars.add(-position-1,ResizeBar(defaultSize, minSize, component, true))
                position
            }
            position >= leadingBars.size -> {
                leadingBars.add(ResizeBar(defaultSize,minSize, component, false))
                leadingBars.size
            }
            else -> {
                leadingBars.add(ResizeBar(defaultSize, minSize, component, false))
                position
            }
        }

        resetLayout()
        return ret
    }

    override fun removePanel( index: Int) {
        TODO()
    }


    private fun resetLayout() {
        this.removeAll()

        layout = CrossLayout.buildCrossLayout(this, {
            leadingBars.forEach { bar ->
                if( bar.componentVisible)
                    rows.add( bar.resizeComponent, height = bar.size)
                rows.add( bar, height = barSize)
            }
            rows.add( stretchComponent, height = minStretch, flex = defaultSize.toFloat())
            trailingBars.forEach {bar ->
                rows.add( bar, height = barSize)
                if( bar.componentVisible)
                    rows.add( bar.resizeComponent, height = bar.size)
            }
            overwriteOrientation = orientation
        })
        validate()
    }

    inner class ResizeBar(
            defaultSize: Int,
            minSize: Int,
            component: IComponent,
            private val trailing: Boolean
    ) : SPanel(), IResizeBar {
        var size : Int = defaultSize ; private set
        override var minSize by LayoutDelegate(minSize)
        override var resizeComponent by LayoutDelegate(component)

        private var componentVisibleBindable = Bindable(true, { resetLayout() })
        var componentVisible by Bound(componentVisibleBindable)

        init {
            val tracker = ResizeBarTracker()
            onMousePress = {tracker.onMousePress(it)}
            onMouseDrag = {tracker.onMouseDrag(it)}

            val btnExpand = SToggleButton(true)
            btnExpand.isBorderPainted = false
            btnExpand.isContentAreaFilled = false
            btnExpand.isFocusPainted = false
            btnExpand.isOpaque = false
            btnExpand.checkBindable.bind(componentVisibleBindable)

            val pullBar = object : SPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)

                    g.color = BarLineColor.color
                    when( orientation) {
                        HORIZONATAL -> {
                            val depth = width
                            val breadth = height
                            g.drawLine( depth/2-2, 10, depth/2-2, breadth-10)
                            g.drawLine( depth/2, 5, depth/2, breadth-5)
                            g.drawLine( depth/2+2, 10, depth/2+2, breadth-10)
                        }
                        VERTICAL -> {
                            val depth = height
                            val breadth = width
                            g.drawLine(10, depth / 2 - 2, breadth - 10, depth / 2 - 2)
                            g.drawLine(5, depth / 2, breadth - 5, depth / 2)
                            g.drawLine(10, depth / 2 + 2, breadth - 10, depth / 2 + 2)
                        }
                    }
                }
            }

            layout =  CrossLayout.buildCrossLayout(this, {
                cols.add( btnExpand, width = 12)
                cols.add( pullBar)
                cols.height = barSize
                overwriteOrientation = if(orientation == VERTICAL) HORIZONATAL else VERTICAL
            })
            pullBar.cursor = if( orientation == HORIZONATAL) Cursor( Cursor.E_RESIZE_CURSOR) else Cursor( Cursor.N_RESIZE_CURSOR )
        }

        internal inner class ResizeBarTracker() {
            var startPos : Int = 0
            var startSize : Int = 0
            var reserved : Int = 0

            fun onMousePress(e: MouseEvent) {
                val p = e.point.convert(this@ResizeContainerPanel)
                reserved = 0

                leadingBars.then(trailingBars)
                        .filter { it != this@ResizeBar }
                        .forEach {
                            reserved += it.size + when( orientation) {
                                HORIZONATAL -> it.width
                                VERTICAL -> it.height
                            }
                        }

                startPos = when( orientation) {
                    HORIZONATAL -> p.x
                    VERTICAL -> p.y
                }

                startSize = size
            }

            fun onMouseDrag(e: MouseEvent) {
                val p = e.point.convert(this@ResizeContainerPanel)

                when( orientation) {
                    HORIZONATAL -> {
                        size = when( trailing) {
                            true -> startSize + (startPos - p.x)
                            false -> startSize - (startPos - p.x)
                        }
                        size = MUtil.clip(minSize, size, this@ResizeContainerPanel.width - minStretch - reserved)
                    }
                    VERTICAL -> {
                        size = when( trailing) {
                            true -> startSize + (startPos - p.y)
                            false -> startSize - (startPos - p.y)
                        }
                        size = MUtil.clip(minSize, size, this@ResizeContainerPanel.height - minStretch - reserved)
                    }
                }

                resetLayout()
            }

        }
    }

    private inner class LayoutDelegate<T>(defaultValue : T) {
        var field = defaultValue

        operator fun getValue(thisRef: Any, prop: KProperty<*>): T = field

        operator fun setValue(thisRef:Any, prop: KProperty<*>, value: T) {
            val old = field
            field = value
            if( old != value)
                resetLayout()
        }
    }
}