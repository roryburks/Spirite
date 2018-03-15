package spirite.gui.basic

import spirite.base.util.MUtil
import spirite.gui.Bindable
import spirite.gui.Orientation

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

class ScrollBarNonUI(
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

    override var maxScroll
        get() = maxScrollBind.field
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

