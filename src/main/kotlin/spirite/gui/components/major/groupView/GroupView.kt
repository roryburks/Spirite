package spirite.gui.components.major.groupView

import spirite.base.brains.IMasterControl
import spirite.base.imageData.groupTree.GroupTree
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

    init {
        val slider = Hybrid.ui.GradientSlider()
        slider.valueBind.addRootListener { new, _ ->  master.centralObservatory.selectedNode.field?.alpha = new}
        master.centralObservatory.selectedNode.addWeakListener { new, _ ->  if( new != null) slider.value = new.alpha}

        panel.setLayout {
            rows.padding = 2
            rows += {
                this.add( Hybrid.ui.Label("Mode: "))
                this.add( Hybrid.ui.ComboBox(spirite.base.graphics.RenderMethodType.values()), height = 16, flex = 100f)
            }
            rows.addGap(2)
            rows.add( slider, height = 24)
        }
    }
}
