package sjunit.spirite.gui

import org.junit.Test
import spirite.gui.advanced.crossContainer.CrossContainer
import spirite.gui.basic.SButton
import spirite.gui.basic.SLabel

class CrossContainerTests {
    @Test
    fun dotest() {
        (0..10).forEach {

        }
        val x = CrossContainer {
            rows += {
                add(SButton("wac"), flex = 1f)
                add(SButton("vscroll"), width = 8)
                flex = 1f
            }
            rows += {
                add(SButton("hscroll"), flex = 1f)
                add(SButton("zoom"), width = 8)
                height = 8
            }
            rows += {
                add(SLabel("CoordinateLabel"))
                addGap(0, 3, Int.MAX_VALUE)
                add(SLabel("MessageLabel"))
                height = 24
            }
        }
    }
}