package spirite.gui.basic

import jspirite.gui.SScrollPane.ModernScrollBarUI
import spirite.base.util.MUtil
import spirite.gui.Bindable
import spirite.gui.Orientation
import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.Orientation.VERTICAL
import javax.swing.JComponent
import javax.swing.JScrollBar

interface IScrollBarNonUI {
    var scroll: Int
    val scrollBind: Bindable<Int>
    var minScroll: Int
    var maxScroll: Int
    var scrollWidth: Int
}

interface IScrollBarNonUIImp : IScrollBarNonUI{
    val minScrollBind : Bindable<Int>
    val maxScrollBind : Bindable<Int>
    var scrollWidthBind : Bindable<Int>
}

interface IScrollBar  : IScrollBarNonUI, IComponent{
    var orientation : Orientation
}

class SScrollBarNonUI(
        min : Int,
        max: Int,
        value: Int,
        width: Int
) : IScrollBarNonUIImp {
    override val scrollBind = Bindable(value)
    override val minScrollBind = Bindable(min)
    override val maxScrollBind = Bindable(max)
    override var scrollWidthBind = Bindable(width)

    override var scroll: Int
        get() = scrollBind.field
        set(to) {
            val clip = MUtil.clip(minScroll, to, maxScroll - scrollWidth)
            scrollBind.field = clip
        }
    override var minScroll
        get() = minScrollBind.field
        set(to) {
            minScrollBind.field = to
            if( maxScroll < to + scrollWidth)
                maxScroll = to + scrollWidth
            if( scroll < maxScroll - scrollWidth)
                scroll = maxScroll - scrollWidth
        }

    override var maxScroll = max
        set(to) {
            maxScrollBind.field = to
            if( minScroll > to - scrollWidth)
                minScroll = to - scrollWidth
            if( scroll > to - scrollWidth)
                scroll = to - scrollWidth
        }


    override var scrollWidth: Int
        get() = scrollWidthBind.field
        set(to) {
            scrollWidthBind.field = to
            if( maxScroll < minScroll + to)
                maxScroll = minScroll + to
            if( scroll > maxScroll - to)
                scroll = maxScroll - to
        }
}

class SScrollBar
private constructor(minScroll: Int, maxScroll: Int, startScroll: Int, scrollWidth: Int, val imp: SScrollBarImp)
    :IScrollBar,
        IScrollBarNonUIImp by SScrollBarNonUI(minScroll, maxScroll, startScroll, scrollWidth),
        ISComponent by SComponentDirect(imp)
{
    constructor(
            orientation: Orientation,
            context: IComponent,
            minScroll: Int = 0,
            maxScroll: Int = 100,
            startScroll: Int = 0,
            scrollWidth : Int = 10) : this( minScroll, maxScroll, startScroll, scrollWidth, SScrollBarImp(orientation, context))


    override var orientation: Orientation
        get() = if( imp.getOrientation() == JScrollBar.VERTICAL) Orientation.VERTICAL else HORIZONATAL
        set(value) { imp.orientation = if( value == VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL}

    init {
        scrollBind.addListener { imp.value = it }
        scrollWidthBind.addListener { imp.visibleAmount = it }
        minScrollBind.addListener { imp.minimum = it}
        maxScrollBind.addListener { imp.maximum = it }
        imp.addAdjustmentListener {scroll = imp.value}
    }

    private class SScrollBarImp(orientation: Orientation, context: IComponent) : JScrollBar() {
        init {
            isOpaque = false
            setUI( ModernScrollBarUI(context as JComponent))
            this.setOrientation(if( orientation == Orientation.VERTICAL) JScrollBar.VERTICAL else JScrollBar.HORIZONTAL)
        }
    }
}