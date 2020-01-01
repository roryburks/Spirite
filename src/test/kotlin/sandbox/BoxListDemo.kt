package sandbox

import sgui.Orientation.HORIZONTAL
import sgui.components.IBoxList.IBoxComponent
import sguiSwing.components.*
import java.awt.Color
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(BoxListDemo())
}

class BoxListDemo : JFrame() {
    init {
        layout = GridLayout()

        val x = SwBoxList(24, 24, (0..100).toList())
        x.renderer = {
            object : IBoxComponent {
                override val component = SwComponent(SJPanel().apply { border = BorderFactory.createRaisedBevelBorder(); add(SwLabel(it.toString()).jcomponent) })

                override fun setSelected(selected: Boolean) {
                    component.component.background = if (selected) Color.BLACK else Color.WHITE
                }
            }
        }
        val resize = ResizeContainerPanel(x, HORIZONTAL)
        resize.minStretch = 100

        var num = 100

        //resize.addPanel(Hybrid.ui.Button("Clear").apply { action = {x.clear()} }, 100,100,-999)
        //resize.addPanel(Hybrid.ui.Button("Add").apply { action = {x.addEntry(++num)} }, 100,100,-999)
        //resize.addPanel(Hybrid.ui.Button("Remove").apply { action = {x.remove(x.selected?:0)} }, 100,100,999)

        add( resize.jcomponent)
    }
}