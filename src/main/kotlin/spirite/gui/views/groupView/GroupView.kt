package spirite.gui.views.groupView

import spirite.base.brains.IMasterControl
import spirite.base.util.ColorARGB32Normal
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.IIcon
import spirite.hybrid.Hybrid

class GroupView
private constructor(
        master: IMasterControl,
        val panel : ICrossPanel)
    : IOmniComponent
{
    override val component: IComponent get() = panel
    override val icon: IIcon? get() = null
    override val name: String get() = "Group Tree"

    constructor(master: IMasterControl) : this(master, panel = Hybrid.ui.CrossPanel())


    init {
        panel.setLayout {
            rows.padding = 2
            rows.add( NodeProperties(master))
            rows.addGap(2)
            val tabPane = Hybrid.ui.TabbedPane()
            tabPane.addTab("Primary", PrimaryGroupView(master))
            rows.add( tabPane, flex = 200f)
            rows.addGap(2)
            rows += {
                add( Hybrid.ui.Button("1"))
                addGap(2)
                add( Hybrid.ui.Button("2"))
                addGap(2)
                add( Hybrid.ui.Button("3"))
                addGap(2)
                add( Hybrid.ui.Button("4"))
            }
        }
    }
}

class NodeProperties
private constructor(
        val master: IMasterControl,
        val panel : ICrossPanel)
    : IComponent by panel
{
    constructor(master: IMasterControl) : this(master,  panel = Hybrid.ui.CrossPanel())

    val slider = Hybrid.ui.GradientSlider()
    val comboBox = Hybrid.ui.ComboBox(spirite.base.graphics.RenderMethodType.values())
    val colorBox = Hybrid.ui.ColorSquare()

    init {
        slider.valueBind.addRootListener { new, _ ->  master.centralObservatory.selectedNode.field?.alpha = new}

        comboBox.selectedItemBind.addRootListener { new, old ->
            val node =  master.centralObservatory.selectedNode.field
            if( node != null) {
                node.method = node.method.copy(methodType = new)
            }
        }

        colorBox.colorBind.addRootListener { new, old ->
            val node =  master.centralObservatory.selectedNode.field
            if( node != null) {
                node.method = node.method.copy(renderValue = new.argb32)
            }
        }



        panel.setLayout {
            rows.padding = 2
            rows += {
                this.add( Hybrid.ui.Label("Mode: "))
                this.add( comboBox, height = 16, flex = 100f)
                this.add(colorBox, 16, 16)
            }
            rows.addGap(2)
            rows.add( slider, height = 24)
        }
    }

    val listener = master.centralObservatory.selectedNode.addWeakListener { new, _ ->
        if( new != null) {
            slider.value = new.alpha
            comboBox.selectedItem = new.method.methodType
            colorBox.color = ColorARGB32Normal(new.method.renderValue)
        }
    }
}
