package spirite.gui.components.basic

import spirite.base.util.binding.CruddyBindable
import rb.vectrix.mathUtil.MathUtil
import spirite.gui.Orientation

interface IScrollBarNonUI {
    var scroll: Int
    val scrollBind: CruddyBindable<Int>
    var minScroll: Int
    var maxScroll: Int
    var scrollWidth: Int
}

interface IScrollBarNonUIImp : IScrollBarNonUI{
    val minScrollBind : CruddyBindable<Int>
    val maxScrollBind : CruddyBindable<Int>
    var scrollWidthBind : CruddyBindable<Int>
}

interface IScrollBar  : IScrollBarNonUI, IComponent{
    var orientation : Orientation
}

class ScrollBarNonUI(
        min : Int,
        max: Int,
        value: Int,
        width: Int
) : IScrollBarNonUIImp {
    override val scrollBind = CruddyBindable(value)
    override val minScrollBind = CruddyBindable(min)
    override val maxScrollBind = CruddyBindable(max)
    override var scrollWidthBind = CruddyBindable(width)

    override var scroll: Int
        get() = scrollBind.field
        set(to) {
            val clip = MathUtil.clip(minScroll, to, maxScroll - scrollWidth)
            scrollBind.field = clip
        }
    override var minScroll
        get() = minScrollBind.field
        set(to) {
            if( minScrollBind.field == to) return
            minScrollBind.field = to
            if( maxScroll < to + scrollWidth)
                maxScroll = to + scrollWidth
            if( scroll < maxScroll - scrollWidth)
                scroll = maxScroll - scrollWidth
        }

    override var maxScroll
        get() = maxScrollBind.field
        set(to) {
            if( maxScrollBind.field == to) return
            maxScrollBind.field = to
            if( minScroll > to - scrollWidth)
                minScroll = to - scrollWidth
            if( scroll > to - scrollWidth)
                scroll = to - scrollWidth
        }


    override var scrollWidth: Int
        get() = scrollWidthBind.field
        set(to) {
            if( scrollWidthBind.field == to) return
            scrollWidthBind.field = to
            if( maxScroll < minScroll + to)
                maxScroll = minScroll + to
            if( scroll > maxScroll - to)
                scroll = maxScroll - to
        }
}

