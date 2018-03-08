package spirite.gui.major.work

import spirite.base.pen.Penner
import jspirite.gui.SScrollPane.ModernScrollBarUI
import spirite.gui.basic.IComponent
import spirite.gui.basic.SLabel
import spirite.gui.basic.SPanel
import java.awt.Font
import java.awt.Graphics
import javax.swing.GroupLayout
import javax.swing.JScrollBar


/**
 *WorkSection is a container for all the elements of the Draw area.  All external
 *	classes should interact with this Form Element (for things like controlling
 *	zoom, etc) instead of going to the inner Panel as this Form controls things
 *	such as scroll bars which feeds information in a one-way scheme.
 *
 *Internal Panels should use WorkPanel to convert screen coordinates to image
 *	coordinates.
 *
 * A note about understanding the various coordinate systems:
 * -The Scrollbar Values correspond directly to the offset of the image in windospace
 *   So if you wanted to draw image point 0,0 in the top-left corner, the scrollbar
 * 	 should be set to the values 0,0.  With the scroll at 1,1, then the point in the top
 * 	 left corner would be <10,10> (with SCROLL_RATIO of 10), meaning the image is drawn
 * 	 with offset <-10,-10>
 *
 * @author Rory Burks
 */
interface IWorkSection {
    val penner: Penner
}

class WorkSection : SPanel(), IComponent {
    val view = WorkSectionView()

    // Region UI
    private val workAreaContainer = SPanel()
    private val coordinateLabel = SLabel()
    private val messageLabel = SLabel()
    private val zoomPanel = object : SPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            when {
                view.zoomLevel >= 0 -> {
                    g.font = Font("Tahoma", Font.PLAIN, 12)
                    g.drawString(Integer.toString(view.zoomLevel + 1), width - if (view.zoomLevel > 8) 16 else 12, height - 5)
                }
                else -> {
                    g.font = Font("Tahoma", Font.PLAIN, 8)
                    g.drawString("1/", this.width - 15, this.height - 10)
                    g.font = Font("Tahoma", Font.PLAIN, 10)
                    g.drawString(Integer.toString(-view.zoomLevel + 1), width - if (view.zoomLevel < -8) 10 else 8, height - 4)
                }
            }
        }
    }

    fun initComponents() {
        val vScroll = JScrollBar()
        vScroll.isOpaque = false
        vScroll.ui = ModernScrollBarUI(this)
        val hScroll = JScrollBar()
        hScroll.isOpaque = false
        hScroll.ui = ModernScrollBarUI(this)
        hScroll.orientation = JScrollBar.HORIZONTAL

        coordinateLabel.text = "Coordinate Label"
        messageLabel.text = "Message Label"

        val layout = GroupLayout(this)


        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(workAreaContainer)
                                        .addComponent(hScroll, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, java.lang.Short.MAX_VALUE.toInt()))
                                .addGroup(layout.createParallelGroup()
                                        .addComponent(vScroll)
                                        // !!!! TODO: figure out why this is necessary to keep the layout working as expected and find a better way
                                        .addComponent(zoomPanel, 0, 0, vScroll.getPreferredSize().width)
                                        // !!!!
                                )
                        )
                        .addGroup(layout.createSequentialGroup()
                                // Bottom Bar
                                .addComponent(coordinateLabel)
                                .addGap(0, 3, java.lang.Short.MAX_VALUE.toInt())
                                .addComponent(messageLabel)
                        )
        )
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(vScroll, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, java.lang.Short.MAX_VALUE.toInt())
                        .addComponent(workAreaContainer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, java.lang.Short.MAX_VALUE.toInt()))
                .addGroup(layout.createParallelGroup()
                        .addComponent(hScroll)
                        .addComponent(zoomPanel, 0, 0, hScroll.getPreferredSize().height)
                )
                .addGroup(layout.createParallelGroup()
                        // Bottom Bar
                        .addComponent(coordinateLabel, 24, 24, 24)
                        .addComponent(messageLabel, 24, 24, 24)
                )
        )
        this.layout = layout

    }
    // endregion


    init {
        initComponents()
    }
}