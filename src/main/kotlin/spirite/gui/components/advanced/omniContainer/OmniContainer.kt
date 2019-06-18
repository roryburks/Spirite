package spirite.gui.components.advanced.omniContainer

import sgui.Orientation
import sgui.Orientation.HORIZONTAL
import sgui.Orientation.VERTICAL
import sgui.components.IComponent
import sgui.components.crossContainer.ICrossPanel
import sguiSwing.components.ResizeContainerPanel
import spirite.hybrid.Hybrid

class OmniContainer
private constructor( init: OmniInitializer.()->Unit, val root: ICrossPanel) : IComponent by root
{
    constructor(init: OmniInitializer.()->Unit) : this(init, Hybrid.ui.CrossPanel())
    init {
        val initObj = OmniInitializer().apply(init)

        root.setLayout {
            rows.add(initObj.makeComponent())
        }
    }

    fun <T> contains() : Boolean {
        TODO()
    }

    fun <T> componentFor() : OmniPart? {
        TODO()
    }

}

class OmniResizeContainer(
        stretchComponent: IComponent,
        orientation: sgui.Orientation,
        defaultSize: Int = 100)
    : ResizeContainerPanel(stretchComponent, orientation, defaultSize)
{

}

sealed class OmniThing{
    internal abstract val component: IComponent
    abstract val minDim: Int
    abstract val prefDim: Int
    abstract val visible: Boolean
}

data class OmniSegment(
        val omniComponent: IOmniComponent,
        override val minDim : Int,
        override val prefDim: Int = minDim,
        override val visible: Boolean = true) : OmniThing()
{
    override val component get() = omniComponent.component
}

class OmniTab(
        val components: List<IOmniComponent>,
        override val minDim : Int,
        override val prefDim: Int = minDim,
        override val visible: Boolean = true) : OmniThing()
{
    override val component: IComponent
        get() = Hybrid.ui.TabbedPane()
                .also { tp -> components.forEach { tp.addTab(it.name, it.component) }  }
}

data class SubContainer(
        override val minDim: Int,
        override val prefDim: Int = minDim,
        override val visible: Boolean = true,
        val init: OmniInitializer.()->Unit) : OmniThing()
{
    override val component: IComponent get() = OmniInitializer().apply(init).makeComponent()
}


class OmniInitializer {
    lateinit var orientation: sgui.Orientation; private set
    lateinit var center : OmniThing
    var defaultSize : Int? = null
    var minSize : Int? = null

    private val leading = mutableListOf<OmniThing>()
    val left : MutableList<OmniThing>
        get()  {
            orientation = HORIZONTAL
            return leading
        }
    val top : MutableList<OmniThing>
        get()  {
            orientation = VERTICAL
            return leading
        }

    private val trailing = mutableListOf<OmniThing>()
    val right : MutableList<OmniThing>
        get()  {
            orientation = HORIZONTAL
            return trailing
        }
    val bottom : MutableList<OmniThing>
        get()  {
            orientation = VERTICAL
            return trailing
        }

    internal fun makeComponent() : IComponent {
        val rc = OmniResizeContainer(center.component, orientation, defaultSize?:100)

        minSize?.also { rc.minStretch = it }

        leading.forEach {rc.addPanel(it.component, it.minDim, it.prefDim, Int.MAX_VALUE, it.visible)}
        trailing.forEach { rc.addPanel(it.component, it.minDim, it.prefDim, Int.MIN_VALUE, it.visible) }

        return rc
    }
}