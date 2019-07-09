package sguiSwing.components

import rb.extendo.extensions.then
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.vectrix.mathUtil.MathUtil
import sgui.Orientation.HORIZONTAL
import sgui.Orientation.VERTICAL
import sgui.components.IComponent
import sgui.components.IResizeContainerPanel
import sgui.components.IResizeContainerPanel.IResizeBar
import sgui.components.crossContainer.ICrossPanel
import sgui.components.events.MouseEvent
import sgui.components.events.MouseEvent.MouseEventType.DRAGGED
import sgui.components.events.MouseEvent.MouseEventType.PRESSED
import sgui.systems.IGlobalMouseHook
import sguiSwing.PrimaryIcon.*
import sguiSwing.SwPrimaryIconSet
import sguiSwing.SwingComponentProvider
import sguiSwing.mouseSystem.SwMouseSystem
import sguiSwing.skin.Skin.ResizePanel.BarLineColor
import kotlin.reflect.KProperty

open class ResizeContainerPanel
private constructor(
        stretchComponent: IComponent,
        orientation: sgui.Orientation,
        private val defaultSize: Int,
        private val panel : ICrossPanel
) : IComponent by panel, IResizeContainerPanel {
    constructor(stretchComponent: IComponent, orientation: sgui.Orientation, defaultSize: Int = 100) : this(stretchComponent, orientation, defaultSize, SwingComponentProvider.CrossPanel())

    override var minStretch: Int by LayoutDelegate(0)
    override var orientation by LayoutDelegate(orientation)
    override var barSize by LayoutDelegate(8)
    override var stretchComponent by LayoutDelegate(stretchComponent)

    private val leadingBars = mutableListOf<ResizeBar>()
    private val trailingBars = mutableListOf<ResizeBar>()

    override fun getPanel( index: Int) : ResizeBar? = when {
        index < 0 && -index <= leadingBars.size -> leadingBars[-index-1]
        index > 0 && index <= trailingBars.size -> trailingBars[index-1]
        else -> null
    }

    override fun addPanel(component: IComponent, minSize: Int, defaultSize: Int, position: Int, visible: Boolean) : Int{
        val p = when(position) {
            0, Int.MAX_VALUE -> leadingBars.size + 1
            Int.MIN_VALUE ->  -trailingBars.size - 1    // -Int.MIN_VALUE = Int.MIN_VALUE.  tricky
            else -> position
        }

        val ret = when {
            p < 0 && -p >= trailingBars.size -> {
                trailingBars.add(ResizeBar(defaultSize, minSize, component, true, visible))
                -trailingBars.size
            }
            p < 0 -> {
                trailingBars.add(-p-1,ResizeBar(defaultSize, minSize, component, true, visible))
                p
            }
            p >= leadingBars.size -> {
                leadingBars.add(ResizeBar(defaultSize,minSize, component, false, visible))
                leadingBars.size
            }
            else -> {
                leadingBars.add(ResizeBar(defaultSize, minSize, component, false, visible))
                p
            }
        }

        resetLayout()
        return ret
    }

    override fun removePanel( index: Int) {
        TODO()
    }


    private fun resetLayout() {
        panel.setLayout {
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
            rows.flex = 100f
            overwriteOrientation = orientation
        }
    }

    inner class ResizeBar
    internal constructor(
            defaultSize: Int,
            minSize: Int,
            component: IComponent,
            private val trailing: Boolean,
            visible : Boolean,
            private val panel: ICrossPanel = SwingComponentProvider.CrossPanel())
        : IComponent by panel, IResizeBar
    {
        var size : Int = defaultSize ; private set
        override var minSize by LayoutDelegate(minSize)
        override var resizeComponent by LayoutDelegate(component)

        private var componentVisibleBindable = Bindable(visible)
                .also{it.addObserver{ _, _ -> resetLayout() }}
        var componentVisible by componentVisibleBindable

        private val _tracker = ResizeBarTracker()
        private val _mouseK = SwMouseSystem.attachHook(object: IGlobalMouseHook {
            override fun processMouseEvent(evt: MouseEvent) {
                if( evt.type == PRESSED) {_tracker.onMousePress(evt)}
                if(evt.type == DRAGGED) {_tracker.onMouseDrag(evt)}
            }

        }, this)

        init {
            val btnExpand =  SwingComponentProvider.ToggleButton(true)
            btnExpand.plainStyle = true
            btnExpand.checkBind.bindTo(componentVisibleBindable)

            when( orientation) {
                VERTICAL -> {
                    btnExpand.setOnIcon(SwPrimaryIconSet.getIcon(SmallArrowE))
                    btnExpand.setOffIcon( SwPrimaryIconSet.getIcon(if( trailing) SmallArrowS else SmallArrowN))
                }
                HORIZONTAL -> {
                    btnExpand.setOnIcon(SwPrimaryIconSet.getIcon(SmallArrowS))
                    btnExpand.setOffIcon( SwPrimaryIconSet.getIcon(if( trailing) SmallArrowE else SmallArrowW))
                }
            }

            // TODO: Figure out the best way to decouple single-use components that require custom drawing/arrangement
            // from any particular UI, even if it means that each UI implementation has to create them from scratch.
            val pullBar = SwPanel { g ->
                g.color = BarLineColor.jcolor
                when (orientation) {
                    HORIZONTAL -> {
                        val depth = width
                        val breadth = height
                        g.drawLine(depth / 2 - 2, 10, depth / 2 - 2, breadth - 10)
                        g.drawLine(depth / 2, 5, depth / 2, breadth - 5)
                        g.drawLine(depth / 2 + 2, 10, depth / 2 + 2, breadth - 10)
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

            panel.setLayout {
                cols.add( btnExpand, width = 12)
                cols.add( pullBar)
                cols.height = barSize
                overwriteOrientation = if(orientation == VERTICAL) HORIZONTAL else VERTICAL
            }
            //pullBar.cursor = if( orientation == HORIZONTAL) Cursor( Cursor.E_RESIZE_CURSOR) else Cursor( Cursor.N_RESIZE_CURSOR )
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
                                HORIZONTAL -> it.width
                                VERTICAL -> it.height
                            }
                        }

                startPos = when( orientation) {
                    HORIZONTAL -> p.x
                    VERTICAL -> p.y
                }

                startSize = size
            }

            fun onMouseDrag(e: MouseEvent) {
                val p = e.point.convert(this@ResizeContainerPanel)

                when( orientation) {
                    HORIZONTAL -> {
                        size = when( trailing) {
                            true -> startSize + (startPos - p.x)
                            false -> startSize - (startPos - p.x)
                        }
                        size = MathUtil.clip(minSize, size, this@ResizeContainerPanel.width - minStretch - reserved)
                    }
                    VERTICAL -> {
                        size = when( trailing) {
                            true -> startSize + (startPos - p.y)
                            false -> startSize - (startPos - p.y)
                        }
                        size = MathUtil.clip(minSize, size, this@ResizeContainerPanel.height - minStretch - reserved)
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