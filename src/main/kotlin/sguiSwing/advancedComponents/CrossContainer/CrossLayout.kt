package sguiSwing.advancedComponents.CrossContainer

import rb.vectrix.mathUtil.floor
import sgui.Orientation.HORIZONTAL
import sgui.Orientation.VERTICAL
import sgui.components.IComponent
import sgui.components.crossContainer.CSE_Component
import sgui.components.crossContainer.CSE_Gap
import sgui.components.crossContainer.CSE_Group
import sgui.components.crossContainer.CrossInitializer
import java.awt.Component
import java.awt.Container
import javax.swing.GroupLayout
import javax.swing.GroupLayout.ParallelGroup

object CrossLayout {
    fun buildCrossLayout(container: Container, componentRecord: MutableList<IComponent>? = null, constructor: CrossInitializer.()->Unit) : GroupLayout {
        val scheme= CrossInitializer().apply { constructor.invoke(this) }.scheme

        val layout = GroupLayout( container)

        if( scheme.rootGroup != null) {
            val pGroup = layout.createParallelGroup()
            val sGroup = layout.createSequentialGroup()

            fun rec(group: CSE_Group, pGroup: ParallelGroup, sGroup: GroupLayout.Group) {
                var sGroup = sGroup
                group.subComponents.forEach {
                    when (it) {
                        is CSE_Gap -> {
                            sGroup.addGap(it.minWidth, it.defaultWidth, it.maxWidth)
                        }
                        is CSE_Component -> {
                            componentRecord?.add(it.component)
                            val comp = it.component.component as Component
                            when {
                                it.fixed != null && it.flex != null -> sGroup.addComponent( comp, it.fixed, it.flex.toInt(), Int.MAX_VALUE)
                                it.fixed != null -> sGroup.addComponent(comp, it.fixed, it.fixed, it.fixed)
                                it.flex != null -> sGroup.addComponent(comp, 0, it.flex.toInt(), Int.MAX_VALUE)
                                else -> sGroup.addComponent(comp)
                            }

                            when {
                                it.overrideGroup != null -> pGroup.addComponent(comp, it.overrideGroup, it.overrideGroup, it.overrideGroup)
                                group.fixed != null -> pGroup.addComponent(comp, group.fixed, group.fixed, group.fixed)
                                group.flex != null -> pGroup.addComponent(comp, 0, group.flex.floor, Int.MAX_VALUE)
                                else -> pGroup.addComponent(comp)
                            }
                        }
                        is CSE_Group -> {
                            val npGroup = layout.createParallelGroup()
                            val nsGroup = layout.createSequentialGroup()

                            when {
                                it.flat -> {
                                    sGroup = layout.createSequentialGroup().apply {
                                        sGroup.addGroup(layout.createParallelGroup()
                                                .addGroup(layout.createSequentialGroup()
                                                        .addGap(it.pregap ?: 0)
                                                        .addGroup(npGroup))
                                                .addGroup(this))
                                    }
                                    pGroup.addGroup(nsGroup)
                                }
                                it.padding != null -> {
                                    pGroup.addGroup(layout.createSequentialGroup()
                                            .addGap(it.padding)
                                            .addGroup(nsGroup)
                                            .addGap(it.padding))
                                    sGroup.addGap(it.padding)
                                            .addGroup(npGroup)
                                            .addGap(it.padding)
                                }
                                else -> {
                                    pGroup.addGroup(nsGroup)
                                    sGroup.addGroup(npGroup)
                                }
                            }
                            rec(it, npGroup, nsGroup)
                        }
                    }
                }
            }

            rec(scheme.rootGroup, pGroup, sGroup)

            // Could be made somewhat cleaner and integrated into the other similar statement within the recursive fun
            val rpGroup : GroupLayout.Group
            val rsGroup : GroupLayout.Group
            when( scheme.rootGroup.padding) {
                null -> {
                    rpGroup = pGroup
                    rsGroup = sGroup
                }
                else -> {
                    rpGroup = layout.createSequentialGroup()
                    rpGroup.addGap(scheme.rootGroup.padding)
                            .addGroup(pGroup)
                            .addGap(scheme.rootGroup.padding)
                    rsGroup = layout.createSequentialGroup()
                    rsGroup.addGap(scheme.rootGroup.padding)
                            .addGroup(sGroup)
                            .addGap(scheme.rootGroup.padding)
                }
            }

            when( scheme.baseOrientation) {
                VERTICAL -> {
                    layout.setVerticalGroup(rsGroup)
                    layout.setHorizontalGroup(rpGroup)
                }
                HORIZONTAL -> {
                    layout.setVerticalGroup(rpGroup)
                    layout.setHorizontalGroup(rsGroup)
                }
            }
        }

        return layout
    }
}