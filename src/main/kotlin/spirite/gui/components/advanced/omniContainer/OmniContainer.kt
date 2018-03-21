package spirite.gui.components.advanced.omniContainer

import spirite.gui.Orientation
import spirite.gui.Orientation.HORIZONTAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.components.advanced.ResizeContainerPanel
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
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

}

class OmniResizeContainer(
        stretchComponent: IComponent,
        orientation: Orientation,
        defaultSize: Int = 100)
    : ResizeContainerPanel(stretchComponent, orientation, defaultSize)
{

}

sealed class OmniThing{
    abstract internal val component: IComponent
    abstract val minDim: Int
    abstract val prefDim: Int
}

data class OmniSegment(
        override val component: IComponent,
        override val minDim : Int,
        override val prefDim: Int = minDim) : OmniThing()

data class SubContainer(
        val init: OmniInitializer.()->Unit,
        override val minDim: Int,
        override val prefDim: Int = minDim) : OmniThing()
{
    override val component: IComponent get() = OmniInitializer().apply(init).makeComponent()
}

class OmniInitializer {
    lateinit var orientation: Orientation ; private set
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

        if( minSize != null) rc.minStretch = minSize!!

        leading.asReversed().forEach {rc.addPanel(it.component, it.minDim, it.prefDim, Int.MAX_VALUE)}
        trailing.forEach { rc.addPanel(it.component, it.minDim, it.prefDim, Int.MIN_VALUE) }

        return rc
    }
}