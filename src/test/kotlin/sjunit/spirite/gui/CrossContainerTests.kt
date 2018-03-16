package sjunit.spirite.gui

import org.junit.Test
import spirite.gui.components.advanced.crossContainer.CrossContainer
import spirite.hybrid.Hybrid

class CrossContainerTests {
    @Test
    fun dotest() {
        (0..10).forEach {

        }
        val x = CrossContainer {
            rows += {
                add(Hybrid.ui.Button("wac"), flex = 1f)
                add(Hybrid.ui.Button("vscroll"), width = 8)
                flex = 1f
            }
            rows += {
                add(Hybrid.ui.Button("hscroll"), flex = 1f)
                add(Hybrid.ui.Button("zoom"), width = 8)
                height = 8
            }
            rows += {
                add(Hybrid.ui.Label("CoordinateLabel"))
                addGap(0, 3, Int.MAX_VALUE)
                add(Hybrid.ui.Label("MessageLabel"))
                height = 24
            }
        }
    }
}