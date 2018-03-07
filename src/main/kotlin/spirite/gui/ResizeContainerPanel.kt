package spirite.gui

import spirite.base.util.MUtil
import spirite.base.util.groupExtensions.then
import spirite.gui.Basic.SButton
import spirite.gui.Basic.SPanel
import spirite.gui.Basic.SToggleButton
import spirite.gui.Bindable.Bound
import spirite.gui.ResizeContainerPanel.ContainerOrientation.HORIZONATAL
import spirite.gui.ResizeContainerPanel.ContainerOrientation.VERTICAL
import java.awt.Cursor
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.math.min
import kotlin.reflect.KProperty

class ResizeContainerPanel(
        stretchComponent: JComponent,
        orientation: ContainerOrientation
) : SPanel() {
    enum class ContainerOrientation {HORIZONATAL, VERTICAL}

    var minStretch: Int by LayoutDelegate(0)
    var orientation by LayoutDelegate(orientation)
    var barSize by LayoutDelegate(8)
    var stretchComponent by LayoutDelegate(stretchComponent)

    private val leadingBars = mutableListOf<ResizeBar>()
    private val trailingBars = mutableListOf<ResizeBar>()

    init {
    }


    fun getPanel( index: Int) : ResizeBar? = when {
        index < 0 && -index <= leadingBars.size -> leadingBars[-index-1]
        index > 0 && index <= trailingBars.size -> trailingBars[index-1]
        else -> null
    }

    fun addPanel( component: JComponent, minSize: Int, defaultSize: Int, position: Int = 0) : Int{
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

    fun removePanel( index: Int) {
        TODO()
    }


    private fun resetLayout() {
        this.removeAll()
        val layout = GroupLayout(this)

        val stretch = layout.createSequentialGroup()
        val noStretch = layout.createParallelGroup(Alignment.LEADING)

        leadingBars.forEach { bar ->
            if( bar.componentVisible) {
                stretch.addComponent(bar.component, bar.size, bar.size, bar.size)
                noStretch.addComponent(bar.component)
            }
            stretch.addComponent( bar, barSize, barSize,barSize)
            noStretch.addComponent(bar)
        }
        stretch.addComponent(stretchComponent, minStretch, minStretch, Short.MAX_VALUE.toInt())
        noStretch.addComponent(stretchComponent)
        trailingBars.forEach { bar ->
            stretch.addComponent( bar, barSize, barSize,barSize)
            if( bar.componentVisible) {
                stretch.addComponent(bar.component, bar.size, bar.size, bar.size)
                noStretch.addComponent(bar.component)
            }
            noStretch.addComponent(bar)
        }

        when( orientation) {
            HORIZONATAL -> {
                layout.setHorizontalGroup(stretch)
                layout.setVerticalGroup(noStretch)
            }
            VERTICAL -> {
                layout.setHorizontalGroup(noStretch)
                layout.setVerticalGroup(stretch)
            }
        }
        setLayout(layout)
        validate()
    }

    inner class ResizeBar(
            defaultSize: Int,
            val minSize: Int,
            val component: JComponent,
            val trailing: Boolean
    ) : SPanel() {
        var size : Int = defaultSize ; private set

        private var componentVisibleBindable = Bindable(true, {resetLayout()})
        var componentVisible by Bound(componentVisibleBindable)

        init {
            val adapter = ResizeBarAdapter()
            addMouseListener(adapter)
            addMouseMotionListener(adapter)

            val btnExpand = SToggleButton(true)
            btnExpand.isBorderPainted = false
            btnExpand.isContentAreaFilled = false
            btnExpand.isFocusPainted = false
            btnExpand.isOpaque = false
            btnExpand.checkBindable.bind(componentVisibleBindable)

            val pullBar = object : SPanel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)

                    g.color = Skin.ResizePanel.BarLineColor.color
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

            val layout = GroupLayout(this)
            when (orientation) {
                HORIZONATAL -> {
                    layout.setHorizontalGroup(layout.createParallelGroup()
                            .addComponent(btnExpand, barSize, barSize, barSize)
                            .addComponent(pullBar, barSize, barSize, barSize)
                    )
                    layout.setVerticalGroup(layout.createSequentialGroup()
                            .addComponent(btnExpand, 12, 12, 12)
                            .addComponent(pullBar)
                    )
                    pullBar.cursor = Cursor(Cursor.E_RESIZE_CURSOR)
                }
                VERTICAL -> {
                    layout.setVerticalGroup(layout.createParallelGroup()
                            .addComponent(btnExpand, barSize, barSize, barSize)
                            .addComponent(pullBar, barSize, barSize, barSize)
                    )
                    layout.setHorizontalGroup(layout.createSequentialGroup()
                            .addComponent(btnExpand, 12, 12, 12)
                            .addComponent(pullBar)
                    )
                    pullBar.cursor = Cursor(Cursor.N_RESIZE_CURSOR)
                }
            }
            this.layout = layout
        }

        internal inner class ResizeBarAdapter : MouseAdapter() {
            var startPos : Int = 0
            var startSize : Int = 0
            var reserved : Int = 0

            override fun mousePressed(e: MouseEvent) {
                val p = SwingUtilities.convertPoint( e.component, e.point, this@ResizeContainerPanel)
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

            override fun mouseDragged(e: MouseEvent) {
                val p = SwingUtilities.convertPoint( e.component, e.point, this@ResizeContainerPanel)

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
                super.mouseDragged(e)
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