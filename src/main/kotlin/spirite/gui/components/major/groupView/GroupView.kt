package spirite.gui.components.major.groupView

import spirite.base.brains.MasterControl
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.hybrid.Hybrid

class GroupView
private constructor(
        master: MasterControl,
        val panel : ICrossPanel)
    : IComponent by panel
{
    constructor(master: MasterControl) : this(master, panel = Hybrid.ui.CrossPanel())


    init {
        panel.setLayout {
            rows.padding = 2
            rows.add( NodeProperties())
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
private constructor( val panel : ICrossPanel)
    : IComponent by panel
{
    constructor() : this( panel = Hybrid.ui.CrossPanel())

    init {
        panel.setLayout {
            rows.padding = 2
            rows += {
                this.add( Hybrid.ui.Label("Mode: "))
                this.add( Hybrid.ui.ComboBox(spirite.base.graphics.RenderMethodType.values()), height = 16, flex = 100f)
            }
            rows.addGap(2)
            rows.add( Hybrid.ui.GradientSlider(), height = 24)
        }
    }
}
