import spirite.gui.Orientation.HORIZONATAL
import spirite.gui.Orientation.VERTICAL
import spirite.gui.advanced.crossContainer.CSE_Component
import spirite.gui.advanced.crossContainer.CSE_Gap
import spirite.gui.advanced.crossContainer.CSE_Group
import spirite.gui.advanced.crossContainer.CrossInitializer
import spirite.gui.basic.ISComponent
import java.awt.Container
import javax.swing.GroupLayout
import javax.swing.GroupLayout.ParallelGroup
import javax.swing.JComponent

object CrossLayout {
    fun buildCrossLayout(container: Container, constructor: CrossInitializer.()->Unit) : GroupLayout {
        val scheme= CrossInitializer().apply { constructor.invoke(this) }.scheme

        val layout = GroupLayout( container)

        if( scheme.rootGroup != null) {
            val pGroup = layout.createParallelGroup()
            val sGroup = layout.createSequentialGroup()

            fun rec(group: CSE_Group, pGroup: ParallelGroup, sGroup: GroupLayout.Group) {
                group.subComponents.forEach {
                    when (it) {
                        is CSE_Gap -> {
                            sGroup.addGap(it.minWidth, it.defaultWidth, it.maxWidth)
                        }
                        is CSE_Component -> {
                            val comp = (it.component as ISComponent).component
                            when {
                                it.fixed != null && it.flex != null -> sGroup.addComponent( comp, it.fixed, it.flex.toInt(), Int.MAX_VALUE)
                                it.fixed != null -> sGroup.addComponent(comp, it.fixed, it.fixed, it.fixed)
                                it.flex != null -> sGroup.addComponent(comp, 0, it.flex.toInt(), Int.MAX_VALUE)
                                else -> sGroup.addComponent(comp)
                            }

                            when {
                                it.overrideGroup != null -> pGroup.addComponent(comp, it.overrideGroup, it.overrideGroup, it.overrideGroup)
                                group.fixed != null -> pGroup.addComponent(comp, group.fixed, group.fixed, group.fixed)
                                else -> pGroup.addComponent(comp, 0, group.flex?.toInt() ?: 100, Int.MAX_VALUE)
                            }
                        }
                        is CSE_Group -> {
                            val npGroup = layout.createParallelGroup()
                            val nsGroup = layout.createSequentialGroup()
                            pGroup.addGroup(nsGroup)
                            sGroup.addGroup(npGroup)
                            rec(it, npGroup, nsGroup)
                        }
                    }
                }
            }

            rec(scheme.rootGroup, pGroup, sGroup)

            when( scheme.baseOrientation) {
                VERTICAL -> {
                    layout.setVerticalGroup(sGroup)
                    layout.setHorizontalGroup(pGroup)
                }
                HORIZONATAL -> {
                    layout.setVerticalGroup(pGroup)
                    layout.setHorizontalGroup(sGroup)
                }
            }
        }

        return layout
    }
}