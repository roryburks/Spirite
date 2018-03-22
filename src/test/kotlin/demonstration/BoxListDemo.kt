package demonstration

import spirite.gui.Orientation.HORIZONTAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.components.advanced.crossContainer.CrossContainer
import spirite.gui.components.advanced.ResizeContainerPanel
import spirite.gui.components.basic.IBoxList.IBoxComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.SwBoxList
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SJPanel
import spirite.pc.gui.basic.SwButton
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.basic.jcomponent
import java.awt.Color
import java.awt.GridLayout
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(BoxListDemo())
}

class BoxListDemo : JFrame() {
    init {
        layout = GridLayout()

        val x = SwBoxList(24,24, (0..100).toList())
        x.renderer = {
            object : IBoxComponent {
                override val component = SwComponent(SJPanel())

                override fun setSelected(selected: Boolean) {
                    component.component.background = if (selected) Color.BLACK else Color.WHITE
                }
            }
        }
        val resize = ResizeContainerPanel(x, HORIZONTAL)
        resize.minStretch = 100


        resize.addPanel(Hybrid.ui.Button("1"), 100,100,-999)
        resize.addPanel(Hybrid.ui.Button("2"), 100,100,-999)
        resize.addPanel(Hybrid.ui.Button("3"), 100,100,999)

        add( resize.jcomponent)
    }
}