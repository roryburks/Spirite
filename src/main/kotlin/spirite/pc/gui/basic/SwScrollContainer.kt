package spirite.pc.gui.basic

import rb.vectrix.mathUtil.d
import spirite.base.util.linear.Rect
import rb.vectrix.mathUtil.round
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IScrollBar
import spirite.gui.components.basic.IScrollContainer
import java.awt.Component

class SwScrollContainer
private constructor( private val imp: SwScrollContainerImp)
    : IScrollContainer, IComponent by SwComponent(imp)
{
    constructor(component: IComponent) : this(SwScrollContainerImp(component.jcomponent))

    class SwScrollContainerImp( val component: Component) : SScrollPane(component) {}

    override val horizontalBar: IScrollBar = SwScrollBar(imp.horizontalScrollBar)
    override val verticalBar: IScrollBar = SwScrollBar(imp.verticalScrollBar)

    init {
        imp.horizontalScrollBar.addAdjustmentListener { evt ->
            horizontalBar.scrollWidth = imp.horizontalScrollBar.visibleAmount
            horizontalBar.minScroll = imp.horizontalScrollBar.minimum
            horizontalBar.maxScroll = imp.horizontalScrollBar.maximum
        }
        imp.verticalScrollBar.addAdjustmentListener { evt ->
            verticalBar.scrollWidth = imp.verticalScrollBar.visibleAmount
            verticalBar.minScroll = imp.verticalScrollBar.minimum
            verticalBar.maxScroll = imp.verticalScrollBar.maximum
        }
    }

    override fun makeAreaVisible(area: Rect) {
        val viewWidth = imp.view.width
        val viewHeight = imp.view.height
        val viewportWidth = imp.viewport.width
        val viewportHeight = imp.viewport.height

        val hBarRatio =
                if( viewWidth == viewportWidth) 1.0
                else (horizontalBar.maxScroll - horizontalBar.scrollWidth) / (viewWidth - viewportWidth).d
        val vBarRatio =
                if( viewHeight == viewportHeight) 1.0
                else (verticalBar.maxScroll - verticalBar.scrollWidth) / (viewHeight - viewportHeight).d

        val view_x1 = horizontalBar.scroll / hBarRatio
        val view_x2 = view_x1 + viewportWidth
        val view_y1 = verticalBar.scroll / hBarRatio
        val view_y2 = view_y1 + viewportHeight

        val x = when {
            area.x < view_x1 -> area.x.d
            area.x2 > view_x2 -> area.x2.d - viewportWidth
            else -> horizontalBar.scroll.d
        }
        val y = when {
            area.y < view_y1 -> area.y.d
            area.y2 > view_y2 -> area.y2.d - viewportHeight
            else -> verticalBar.scroll.d
        }
        horizontalBar.scroll = (x * hBarRatio).round
        verticalBar.scroll = (y*vBarRatio).round
    }
}